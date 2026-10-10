import java.util.Scanner
import java.util.Locale
import kotlin.properties.Delegates

class InsufficientFundsException(message: String) : Exception(message)
interface PaymentMethod
class CardPayment(val last4Digits: String) : PaymentMethod
class CryptoPayment(val walletAddress: String) : PaymentMethod

fun Double.formatAsCurrency(): String {
    return String.format(Locale.US, "%,.2f NC", this).replace(',', ' ')
}

infix fun Double.formattedAs(currency: String): String {
    val rounded = String.format(Locale.US, "%.2f", this)
    return "$rounded $currency"
}

class SmartWallet(
    val owner: String?,
    initialBalance: Double
) {
    var balance: Double by Delegates.observable(initialBalance) { _, old, new ->
        println("Изменение баланса: $old -> $new")
    }

    val auditInfo: String by lazy {
        println("  📋 (формирование финансового отчёта...)")
        """
        Владелец: ${owner ?: "Анонимный пользователь"}      
        |Текущий баланс: ${balance.formatAsCurrency()}
        """.trimMargin()
    }

    operator fun plus(amount: Double): SmartWallet {
        require(amount >= 0) { "Сумма пополнения должна быть положительной" }
        balance += amount
        return this
    }

    operator fun minus(amount: Double): SmartWallet {
        if (amount <= 0) {
            throw InsufficientFundsException(
                "Сумма снятия должна быть положительной. Получено: $amount"
            )
        }
        if (amount > balance) {
            throw InsufficientFundsException(
                """
                |Недостаточно денег
                |Владелец: ${owner ?: "Анонимный пользователь"}
                |Текущий баланс: ${balance.formatAsCurrency()}
                |Не хватает: ${amount - balance}
                """.trimMargin()
            )
        }
        balance -= amount
        return this
    }

    override fun toString(): String =
        "SmartWallet(owner=${owner ?: "Аноним"}, balance=${balance.formatAsCurrency()})"
}

class WalletBuilder {
    var owner: String? = null
    var initialBalance: Double = 0.0
}

fun createWallet(block: WalletBuilder.() -> Unit): SmartWallet {
    val builder = WalletBuilder()
    builder.block()
    return SmartWallet(builder.owner, builder.initialBalance)
}

fun readDouble(scanner: Scanner): Double {
    while (true) {
        try {
            val input = scanner.next().replace(',', '.')
            val value = input.toDoubleOrNull()
            if (value != null && value >= 0) return value
            println("  ❌ Ошибка: введите неотрицательное число.")
        } catch (e: Exception) {
            println("  ❌ Ошибка ввода. Попробуйте снова.")
        }
    }
}

fun main() {
    val scanner = Scanner(System.`in`)

    println("Симуляция")

    // ВВОД С КЛАВИАТУРЫ
    println("\n📋 ВВОД ДАННЫХ КОШЕЛЬКА:")

    print("Введите имя владельца (или оставьте пустым для анонима): ")
    val ownerInput = scanner.nextLine().trim()
    val _owner: String? = if (ownerInput.isEmpty()) null else ownerInput

    print("Введите начальный баланс: ")
    val _initialBalance = readDouble(scanner)

    println("\n📋 ШАГ 1: Создание кошелька")

    // DSL-вызов — теперь без путаницы
    val wallet = createWallet {
        owner = _owner
        initialBalance = _initialBalance
    }.apply {
        println("  ✅ Кошелёк создан")
    }.also {
        println("  📝 Владелец = ${it.owner ?: "Анонимный пользователь"}")
    }

    println("  Итог: $wallet")

    println("\n📋 ШАГ 2: Пополнение")
    print("Введите сумму пополнения: ")
    val depositAmount = readDouble(scanner)
    wallet + depositAmount

    println("\n📋 ШАГ 3: Безопасная проверка владельца через let")

    wallet.owner?.let { name ->
        println("  👤 Владелец верифицирован: $name")
    } ?: println("  👤 Владелец анонимный")

    println("\n📋 ШАГ 4: Снятие средств")
    print("Введите сумму снятия: ")
    val withdrawAmount = readDouble(scanner)

    try {
        wallet - withdrawAmount
    } catch (e: InsufficientFundsException) {
        println("  ❌ Перехвачено исключение: ${e.message}")
    }

    println("\nПроверка типа платежа через is + as?")

    val payment: PaymentMethod = CryptoPayment("0xABCD1234EFGH5678")

    if (payment is CryptoPayment) {
        val address = payment as? CryptoPayment
        println("  💰 Криптокошелёк: ${address?.walletAddress}")
    } else if (payment is CardPayment) {
        val card = payment as? CardPayment
        println("  💳 Карта: ****${card?.last4Digits}")
    }

    println("\nПолучение auditInfo")

    println(wallet.auditInfo)

    println("\nformattedAs:")
    println(_initialBalance formattedAs "NC")
    println("\nformatAsCurrency:")
    println(_initialBalance.formatAsCurrency())
}