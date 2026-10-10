import java.util.Scanner
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.random.Random

// ============================================================
// УРОН
// ============================================================
sealed class Damage(val amount: Int) {
    abstract val typeName: String
}

class PhysicalDamage(amount: Int) : Damage(amount) {
    override val typeName: String = "физического"
}

class MagicalDamage(amount: Int) : Damage(amount) {
    override val typeName: String = "магического"
}

class PoisonDamage(amount: Int) : Damage(amount) {
    override val typeName: String = "яда"
}

class FireDamage(amount: Int) : Damage(amount) {
    override val typeName: String = "огня"
}

// ============================================================
// ПИТОМЕЦ
// ============================================================
class Pet(
    val name: String,
    val damage: Int,
    var health: Int = 10,
    val maxHealth: Int = 10
) {
    val isAlive: Boolean get() = health > 0

    fun takeDamage(amount: Int) {
        health = (health - amount).coerceAtLeast(0)
    }

    override fun toString(): String =
        "Pet(name=$name, damage=$damage, health=$health/$maxHealth)"
}

// ============================================================
// ТИПЫ ПРЕДМЕТОВ
// ============================================================
enum class ItemType(val displayName: String) {
    POTION("зелье лечения"),
    STRENGTH_POTION("зелье силы"),
    POISONOUS_POTION("ядовитое зелье"),
    WEAPON("оружие"),
    RING("кольцо ловкости"),
    ARMOR_REPAIR("ремонт брони"),
    SCROLL("свиток огня"),
    DOG_WHISTLE("собачий свисток")
}

// ============================================================
// ПРЕДМЕТ
// ============================================================
class Item(val name: String, val type: ItemType, val effect: Int, val price: Int = 0)
{
    fun copy(
        name: String = this.name,
        type: ItemType = this.type,
        effect: Int = this.effect,
        price: Int = this.price
    ): Item = Item(name, type, effect, price)

    override fun toString(): String =
        "Item(name=$name, type=$type, effect=$effect, price=$price)"
}

// ============================================================
// БАЗОВЫЙ ПЕРСОНАЖ
// ============================================================
abstract class Character(val name: String, maxHealth: Int)
{
    var health: Int = maxHealth
        protected set

    val maxHealth: Int = maxHealth

    private val _inventory = mutableListOf<Item>()
    val inventory: List<Item> get() = _inventory.toList()

    var weaponBonus: Int = 0
        protected set

    var strengthBonus: Int = 0
        protected set

    var gold: Int = 120
        protected set

    private var burnTurns: Int = 0
    private var burnDamage: Int = 0

    val isBurning: Boolean
        get() = burnTurns > 0

    val pets = mutableListOf<Pet>()

    val hasPet: Boolean
        get() = pets.isNotEmpty()

    val petCount: Int
        get() = pets.size

    abstract fun getAttackDamage(): Int
    abstract fun attack(target: Character)

    open fun block(): Int {
        return 0   // по умолчанию — нет блока
    }

    open fun castSpell(target: Character) {
        println("  ❌ $name не умеет колдовать!")
    }

    fun summonPet(newPet: Pet) {
        if (pets.size >= 2) {
            println("  ❌ Нельзя призвать больше двух волков!")
            return
        }
        pets.add(newPet)
        println("  🐺 $name призывает ${newPet.name}! (волков: ${pets.size}/2)")
    }

    fun petAttack(target: Character) {
        if (pets.isEmpty()) return
        pets.forEach { pet ->
            println("  🐺 ${pet.name} атакует ${target.name} и наносит ${pet.damage} физического урона!")
            target.takeDamage(PhysicalDamage(pet.damage))
        }
    }

    fun petTakeDamage(amount: Int, index: Int) {
        if (index !in pets.indices) return
        val pet = pets[index]
        pet.takeDamage(amount)
        println("  🐺 ${pet.name} #${index + 1} получает $amount урона! HP: ${pet.health}/${pet.maxHealth}")
        if (!pet.isAlive) {
            println("  💀 ${pet.name} #${index + 1} пал в бою!")
            pets.removeAt(index)
        }
    }

    fun applyBurn(damage: Int, turns: Int) {
        burnDamage = damage
        burnTurns = turns
        println("  🔥 $name охвачен пламенем! $damage урона в течение $turns ходов.")
    }

    fun tickBurn() {
        if (burnTurns > 0) {
            println("  🔥 Пламя обжигает $name: $burnDamage урона огня! (осталось ходов: ${burnTurns - 1})")
            takeDamage(FireDamage(burnDamage))
            burnTurns--
            if (burnTurns == 0) {
                println("  💨 Пламя на $name угасает.")
            }
        }
    }

    val isAlive: Boolean
        get() = health > 0

    open fun takeDamage(damage: Damage) {
        health = (health - damage.amount).coerceAtLeast(0)
    }

    fun addItem(item: Item) {
        _inventory.add(item)
    }

    fun addGold(amount: Int) {
        gold += amount
    }

    fun findItem(type: ItemType): Int {
        return inventory.indexOfFirst { it.type == type }
    }

    open fun restoreForNewBattle() {
        health = maxHealth
        weaponBonus = 0
        strengthBonus = 0
        burnTurns = 0
        burnDamage = 0
        pets.clear()
        println("  ✨ $name восстановлен! HP: $health/$maxHealth")
    }

    fun attackPet(target: Character, index: Int) {
        val damage = getAttackDamage()
        println("⚔️ $name атакует питомца ${target.name} и наносит $damage урона!")
        target.petTakeDamage(damage, index)
    }

    open fun useItem(index: Int, target: Character? = null): Boolean {
        if (index !in _inventory.indices) {
            println("  ❌ Неверный индекс предмета: $index")
            return false
        }
        val item = _inventory[index]
        when (item.type) {
            ItemType.POTION -> {
                val healed = (health + item.effect).coerceAtMost(maxHealth)
                val actualHeal = healed - health
                health = healed
                println("  🧪 $name выпивает ${item.name} и восстанавливает $actualHeal HP. Теперь: $health/$maxHealth HP")
            }
            ItemType.STRENGTH_POTION -> {
                strengthBonus += item.effect
                println("  💪 $name выпивает ${item.name}. Бонус к урону: +${item.effect} (всего: +$strengthBonus)")
            }
            ItemType.POISONOUS_POTION -> {
                if (target == null) {
                    println("  ❌ Нужна цель для ядовитого зелья! Пропуск хода.")
                    return false
                }
                println("  🧪 $name кидает ${item.name} в ${target.name}!")
                println("  ☠️ ${target.name} получает ${item.effect} урона ядом!")
                target.takeDamage(PoisonDamage(item.effect))
            }
            ItemType.SCROLL -> {
                if (target == null) {
                    println("  ❌ Нужна цель для свитка огня! Пропуск хода.")
                    return false
                }
                println("  📜 $name читает ${item.name} и насылает пламя на ${target.name}!")
                target.applyBurn(item.effect, 3)
            }
            ItemType.WEAPON -> {
                weaponBonus += item.effect
                println("  ⚔️ $name экипирует ${item.name}. Бонус к урону: +${item.effect}")
            }
            ItemType.RING -> {
                weaponBonus += item.effect
                println("  💍 $name надевает ${item.name}. Бонус к урону: +${item.effect}")
            }
            ItemType.ARMOR_REPAIR -> {
                println("  🔧 $name использует ${item.name}. (Только для воинов)")
            }
            ItemType.DOG_WHISTLE -> {
                if (target == null) {
                    println("  ❌ Нужна цель для свистка! Пропуск хода.")
                    return false
                }
                println("  🎺 $name свистит в ${item.name}...")
                val roll = Random.nextInt(100)
                if (roll < 50) {
                    println("  ⚡ С небес спускается БОЖЕСТВЕННЫЙ ПЁС ФЕНРИР!")
                    println("  ☄️ Фенрир обрушивает на ${target.name} 50 урона и исчезает!")
                    target.takeDamage(PhysicalDamage(50))
                } else {
                    println("  🐺 Из леса выбегает ВОЛК и присоединяется к бою!")
                    summonPet(Pet("Волк", 5, 10, 10))
                }
            }
            else -> {
                println("  ❌ Неизвестный тип предмета!")
                return false
            }
        }
        _inventory.removeAt(index)
        return true
    }

    fun showInventory() {
        if (_inventory.isEmpty()) {
            println("  🎒 Инвентарь $name пуст. (золото: $gold)")
        } else {
            println("  🎒 Инвентарь $name (золото: $gold):")
            _inventory.forEachIndexed { i, item ->
                println("     [${i + 1}] ${item.name} (${item.type.displayName}, эффект: ${item.effect})")
            }
        }
    }

    override fun toString(): String = "$name ($health/$maxHealth HP)"
}

// ============================================================
// ВОИН
// ============================================================
class Warrior(
    name: String,
    maxHealth: Int,
    private var armor: Int,
    private val maxArmor: Int = armor
) : Character(name, maxHealth) {


    override fun getAttackDamage(): Int = 15 + weaponBonus + strengthBonus

    override fun block(): Int = armor   // ← вместо интерфейса

    override fun takeDamage(damage: Damage) {
        when (damage) {
            is PhysicalDamage -> {
                val reduced = (damage.amount - armor).coerceAtLeast(1)
                val absorbed = damage.amount - reduced

                if (absorbed > 0) {
                    armor = (armor - absorbed / 2).coerceAtLeast(0)
                    println("  🛡️ Броня $name поглощает $absorbed урона (${damage.amount} → $reduced)")
                    if (armor < maxArmor) {
                        println("  🔧 Броня изнашивается! Осталось: $armor/$maxArmor")
                    }
                } else {
                    println("  🛡️ Броня $name не справляется, но минимальный урон 1 проходит")
                }
                super.takeDamage(PhysicalDamage(reduced))
            }
            is MagicalDamage -> {
                val increased = ceil(damage.amount * 1.2).toInt()
                println("  🛡️ Броня $name не защищает от магии! Уязвимость: +20% (${damage.amount} → $increased)")
                super.takeDamage(MagicalDamage(increased))
            }
            is PoisonDamage -> {
                println("  🛡️ Броня $name не защищает от яда!")
                super.takeDamage(damage)
            }
            is FireDamage -> {
                println("  🛡️ Броня $name не защищает от огня!")
                super.takeDamage(damage)
            }
        }
    }

    fun repairArmor(amount: Int) {
        armor = (armor + amount).coerceAtMost(maxArmor)
        println("  🔧 $name ремонтирует броню. Теперь: $armor/$maxArmor")
    }

    override fun restoreForNewBattle() {
        super.restoreForNewBattle()
        armor = maxArmor
        println("  🔧 Броня $name восстановлена: $armor/$maxArmor")
    }

    override fun attack(target: Character) {
        val damage = getAttackDamage()
        println("⚔️ $name наносит $damage физического урона ${target.name}.")
        target.takeDamage(PhysicalDamage(damage))
    }

    override fun useItem(index: Int, target: Character?): Boolean {
        if (index in inventory.indices && inventory[index].type == ItemType.ARMOR_REPAIR) {
            repairArmor(inventory[index].effect)
        }
        return super.useItem(index, target)
    }
}

// ============================================================
// ГОБЛИН
// ============================================================
class Goblin(
    name: String,
    maxHealth: Int
) : Character(name, maxHealth) {

    override fun getAttackDamage(): Int = 18 + weaponBonus + strengthBonus

    override fun takeDamage(damage: Damage) {
        when (damage) {
            is MagicalDamage -> {
                val halved = floor(damage.amount / 2.0).toInt()
                println("  🟢 Расовая особенность гоблина: магический урон уменьшен вдвое (${damage.amount} → $halved)")
                super.takeDamage(MagicalDamage(halved))
            }
            is PhysicalDamage -> super.takeDamage(damage)
            is PoisonDamage -> super.takeDamage(damage)
            is FireDamage -> super.takeDamage(damage)
        }
    }

    override fun attack(target: Character) {
        val damage = getAttackDamage()
        println("👹 $name кусает ${target.name} и наносит $damage физического урона.")
        target.takeDamage(PhysicalDamage(damage))
    }
}

// ============================================================
// МАГ
// ============================================================
class Mage(
    name: String,
    maxHealth: Int,
    private val magicPower: Int
) : Character(name, maxHealth) {

    override fun getAttackDamage(): Int = magicPower + strengthBonus

    override fun castSpell(target: Character) {   // ← вместо интерфейса
        val damage = getAttackDamage()
        println("✨ $name колдует на ${target.name}, нанося $damage магического урона.")
        target.takeDamage(MagicalDamage(damage))
    }

    override fun takeDamage(damage: Damage) {
        when (damage) {
            is PhysicalDamage -> {
                val increased = ceil(damage.amount * 1.2).toInt()
                println("  🔮 Маг хрупок: физический урон увеличен на 20% (${damage.amount} → $increased)")
                super.takeDamage(PhysicalDamage(increased))
            }
            else -> super.takeDamage(damage)
        }
    }

    override fun attack(target: Character) {
        castSpell(target)
    }
}

// ============================================================
// МАГАЗИН
// ============================================================
class Shop {
    private val items = listOf(
        Item("Зелье лечения", ItemType.POTION, 30, 50),
        Item("Зелье силы", ItemType.STRENGTH_POTION, 10, 80),
        Item("Ядовитое зелье", ItemType.POISONOUS_POTION, 25, 60),
        Item("Стальной меч", ItemType.WEAPON, 10, 100),
        Item("Кольцо ловкости", ItemType.RING, 5, 120),
        Item("Ремонт брони", ItemType.ARMOR_REPAIR, 5, 40),
        Item("Свиток огня", ItemType.SCROLL, 15, 90),
        Item("Собачий свисток", ItemType.DOG_WHISTLE, 0, 70)
    )

    fun showItems(character: Character) {
        println("\n🏪 МАГАЗИН:")
        println("-".repeat(60))
        items.forEachIndexed { i, item ->
            val available = when (item.type) {
                ItemType.ARMOR_REPAIR -> character is Warrior
                ItemType.SCROLL -> character is Mage
                ItemType.DOG_WHISTLE -> character is Goblin
                else -> true
            }
            val marker = if (available) "" else when (item.type) {
                ItemType.ARMOR_REPAIR -> "  ⛔ (только для воинов)"
                ItemType.SCROLL -> "  ⛔ (только для магов)"
                ItemType.DOG_WHISTLE -> "  ⛔ (только для гоблинов)"
                else -> ""
            }
            println("  [${i + 1}] ${item.name} (${item.type.displayName}, эффект: ${item.effect}) — ${item.price} золота$marker")
        }
        println("  [0] Выйти из магазина")
        println("-".repeat(60))
    }

    fun buy(character: Character, index: Int): Boolean {
        if (index !in items.indices) return false
        val item = items[index]

        if (item.type == ItemType.ARMOR_REPAIR && character !is Warrior) {
            println("  ❌ ${character.name}, вы не воин — ремонт брони вам недоступен!")
            return false
        }

        if (item.type == ItemType.SCROLL && character !is Mage) {
            println("  ❌ ${character.name}, вы не маг — свиток огня вам недоступен!")
            return false
        }

        if (item.type == ItemType.DOG_WHISTLE && character !is Goblin) {
            println("  ❌ ${character.name}, вы не гоблин — собачий свисток вам недоступен!")
            return false
        }

        if (character.gold < item.price) {
            println("  ❌ Недостаточно золота! Нужно: ${item.price}, есть: ${character.gold}")
            return false
        }
        character.addGold(-item.price)
        character.addItem(item.copy())
        println("  ✅ Куплено: ${item.name} за ${item.price} золота")
        return true
    }

    fun itemCount(): Int = items.size
}

// ============================================================
// ИНТЕРАКТИВНАЯ АРЕНА
// ============================================================
class InteractiveArena(
    private val player: Character,
    private val enemy: Character,
    private val scanner: Scanner
) {
    fun fight(): Boolean {
        println("\n" + "=".repeat(60))
        println("🏟️ АРЕНА ТЕНЕЙ: ${player.name} VS ${enemy.name}")
        println("=".repeat(60))

        var round = 1

        while (player.isAlive && enemy.isAlive) {
            println("\n" + "-".repeat(60))
            println("📊 РАУНД $round")
            println("-".repeat(60))

            val playerPets = if (player.hasPet) " 🐺×${player.petCount}" else ""
            val enemyPets = if (enemy.hasPet) " 🐺×${enemy.petCount}" else ""

            println("  🧙 ${player.name}: ${player.health}/${player.maxHealth} HP" +
                    (if (player.isBurning) " 🔥" else "") + playerPets)
            println("  👹 ${enemy.name}: ${enemy.health}/${enemy.maxHealth} HP" +
                    (if (enemy.isBurning) " 🔥" else "") + enemyPets)
            println("-".repeat(60))

            println("\n🎮 ВАШ ХОД:")
            val fled = playerTurn()
            if (fled) return false

            if (!enemy.isAlive) {
                println("\n💀 ${enemy.name} пал в бою!")
                break
            }

            if (player.hasPet) {
                println("\n🐺 ХОД ПИТОМЦЕВ:")
                player.petAttack(enemy)
                if (!enemy.isAlive) {
                    println("\n💀 ${enemy.name} пал от клыков питомцев!")
                    break
                }
            }

            if (enemy.isBurning) {
                println("\n🔥 ЭФФЕКТ ГОРЕНИЯ (перед ходом врага):")
                enemy.tickBurn()
                if (!enemy.isAlive) {
                    println("\n💀 ${enemy.name} сгорел в пламени!")
                    break
                }
            }

            println("\n👹 ХОД ВРАГА:")
            enemyTurn()

            if (!player.isAlive) {
                println("\n💀 ${player.name} пал в бою!")
                break
            }

            if (enemy.hasPet) {
                println("\n🐺 ХОД ПИТОМЦЕВ ВРАГА:")
                enemy.petAttack(player)
                if (!player.isAlive) {
                    println("\n💀 ${player.name} пал от клыков питомцев!")
                    break
                }
            }

            if (player.isBurning) {
                println("\n🔥 ЭФФЕКТ ГОРЕНИЯ (перед вашим ходом):")
                player.tickBurn()
                if (!player.isAlive) {
                    println("\n💀 ${player.name} сгорел в пламени!")
                    break
                }
            }

            round++
        }

        println("\n" + "=".repeat(60))
        if (player.isAlive) {
            println("🏆 ПОБЕДИТЕЛЬ: ${player.name} (${player.health}/${player.maxHealth} HP)")
            return true
        } else {
            println("🏆 ПОБЕДИТЕЛЬ: ${enemy.name} (${enemy.health}/${enemy.maxHealth} HP)")
            return false
        }
    }

    private fun playerTurn(): Boolean {
        println("  Выберите действие:")
        println("    1 — ⚔️ Атаковать")
        println("    2 — 🧪 Использовать предмет")
        println("    3 — 🎒 Показать инвентарь")
        println("    4 — 🏃 Бежать")

        return when (readInt(scanner)) {
            1 -> { chooseTarget(); false }
            2 -> { useItemMenu(); false }
            3 -> {
                player.showInventory()
                println("  (Ход НЕ потрачен — можете сходить ещё раз)")
                playerTurn()
                false
            }
            4 -> {
                println("  🏃 ${player.name} покидает арену!")
                println("\n🏳️ ПОБЕГ! Бой завершён.")
                println("🏆 Формальный победитель: ${enemy.name}")
                println("=".repeat(60))
                true
            }
            else -> {
                println("  ❌ Неверный выбор! Пропуск хода.")
                false
            }
        }
    }

    private fun chooseTarget() {
        if (!enemy.hasPet) {
            player.attack(enemy)
            return
        }

        println("\n  🎯 ВЫБЕРИТЕ ЦЕЛЬ:")
        println("    1 — 👹 ${enemy.name} (${enemy.health}/${enemy.maxHealth} HP)")
        enemy.pets.forEachIndexed { i, pet ->
            println("    ${i + 2} — 🐺 ${pet.name} #${i + 1} (${pet.health}/${pet.maxHealth} HP)")
        }
        print("  Ваш выбор: ")

        val choice = readInt(scanner)
        when {
            choice == 1 -> player.attack(enemy)
            choice in 2..(1 + enemy.petCount) -> player.attackPet(enemy, choice - 2)
            else -> {
                println("  ❌ Неверный выбор! Атакуем противника.")
                player.attack(enemy)
            }
        }
    }

    private fun useItemMenu() {
        if (player.inventory.isEmpty()) {
            println("  ❌ Инвентарь пуст! Пропуск хода.")
            return
        }
        player.showInventory()
        print("  Введите номер предмета (1-${player.inventory.size}): ")
        val choice = readInt(scanner)
        val index = choice - 1
        if (index !in player.inventory.indices) {
            println("  ❌ Неверный номер. Пропуск хода.")
            return
        }
        player.useItem(index, enemy)
    }

    private fun enemyTurn() {
        if (enemy.health < enemy.maxHealth * 0.4) {
            val potionIndex = enemy.findItem(ItemType.POTION)
            if (potionIndex != -1) {
                println("  🤖 Враг решает выпить зелье!")
                enemy.useItem(potionIndex, player)
                return
            }
        }

        if (enemy.inventory.isNotEmpty() && Random.nextInt(100) < 20) {
            val index = Random.nextInt(enemy.inventory.size)
            val item = enemy.inventory[index]
            println("  🤖 Враг использует ${item.name}!")
            enemy.useItem(index, player)
            return
        }

        if (player.hasPet && Random.nextInt(100) < 30) {
            val petIndex = Random.nextInt(player.petCount)
            enemy.attackPet(player, petIndex)
        } else {
            enemy.attack(player)
        }
    }
}

// ============================================================
// СОЗДАНИЕ ПЕРСОНАЖА
// ============================================================
fun createPlayer(choice: Int): Character {
    return when (choice) {
        1 -> Warrior("Воин", 100, armor = 10).also {
            it.addItem(Item("Зелье лечения", ItemType.POTION, 30))
            it.addItem(Item("Стальной меч", ItemType.WEAPON, 8))
        }
        2 -> Goblin("Гоблин", 60).also {
            it.addItem(Item("Зелье лечения", ItemType.POTION, 20))
            it.addItem(Item("Кольцо ловкости", ItemType.RING, 5))
        }
        3 -> Mage("Маг", 70, magicPower = 25).also {
            it.addItem(Item("Зелье лечения", ItemType.POTION, 25))
            it.addItem(Item("Свиток огня", ItemType.SCROLL, 15))
        }
        else -> Warrior("Воин", 100, armor = 10).also {
            it.addItem(Item("Зелье лечения", ItemType.POTION, 30))
            it.addItem(Item("Ядовитое зелье", ItemType.POISONOUS_POTION, 25))
        }
    }
}

// ============================================================
// СОЗДАНИЕ ВРАГА
// ============================================================
fun createEnemy(choice: Int): Character {
    return when (choice) {
        1 -> Warrior("Тёмный рыцарь", 100, armor = 10).also {
            it.addItem(Item("Зелье лечения", ItemType.POTION, 30))
            it.addItem(Item("Стальной меч", ItemType.WEAPON, 8))
            it.addItem(Item("Ремонт брони", ItemType.ARMOR_REPAIR, 5))
        }
        2 -> Goblin("Гоблин-воин", 60).also {
            it.addItem(Item("Зелье лечения", ItemType.POTION, 20))
            it.addItem(Item("Кольцо ловкости", ItemType.RING, 5))
            it.addItem(Item("Собачий свисток", ItemType.DOG_WHISTLE, 0))
        }
        3 -> Mage("Тёмный маг", 70, magicPower = 25).also {
            it.addItem(Item("Зелье лечения", ItemType.POTION, 25))
            it.addItem(Item("Свиток огня", ItemType.SCROLL, 15))
        }
        else -> Goblin("Гоблин-воин", 60).also {
            it.addItem(Item("Зелье лечения", ItemType.POTION, 20))
            it.addItem(Item("Кольцо ловкости", ItemType.RING, 5))
            it.addItem(Item("Собачий свисток", ItemType.DOG_WHISTLE, 0))
        }
    }
}

// ============================================================
// МАГАЗИН
// ============================================================
fun visitShop(shop: Shop, player: Character, scanner: Scanner) {
    println("\n🏪 МАГАЗИН")
    println("  Ваше золото: ${player.gold}")
    println("  (Совет: купите зелья и улучшения перед боем!)")

    while (true) {
        shop.showItems(player)
        print("  Ваш выбор: ")
        val choice = readInt(scanner)

        if (choice == 0) {
            println("  👋 Вы вышли из магазина.")
            break
        }

        val index = choice - 1
        if (index in 0 until shop.itemCount()) {
            shop.buy(player, index)
        } else {
            println("  ❌ Неверный выбор.")
        }
    }
}

// ============================================================
// ГЛАВНАЯ ФУНКЦИЯ
// ============================================================
fun main() {
    val scanner = Scanner(System.`in`)

    try {
        println("=".repeat(60))
        println("🎮 АРЕНА ТЕНЕЙ — ИНТЕРАКТИВНЫЙ БОЙ")
        println("=".repeat(60))

        println("\n📋 ВЫБЕРИТЕ ПЕРСОНАЖА:")
        println("-".repeat(60))
        println("  1 — ⚔️ Воин (100 HP, броня 10, урон 15)")
        println("  2 — 👹 Гоблин (60 HP, урон 18, магия вдвое)")
        println("  3 — 🔮 Маг (70 HP, урон 25 магией, хрупкий)")
        println("-".repeat(60))
        print("Ваш выбор: ")

        val playerChoice = readInt(scanner)
        var player = createPlayer(playerChoice)
        println("\n✅ Вы выбрали: ${player.name}")

        val shop = Shop()
        var totalBattles = 0
        var continueGame = true

        while (continueGame) {
            visitShop(shop, player, scanner)

            println("\n📋 ВЫБЕРИТЕ ПРОТИВНИКА:")
            println("-".repeat(60))
            println("  1 — ⚔️ Воин (100 HP, броня 10)")
            println("  2 — 👹 Гоблин (60 HP, урон 18)")
            println("  3 — 🔮 Маг (70 HP, урон 25 магией)")
            println("-".repeat(60))
            print("Ваш выбор: ")

            val enemyChoice = readInt(scanner)
            val enemy = createEnemy(enemyChoice)
            println("\n✅ Противник: ${enemy.name}")

            println("\n🎒 ВАШ ИНВЕНТАРЬ:")
            player.showInventory()

            println("\n🎒 ИНВЕНТАРЬ ПРОТИВНИКА:")
            enemy.showInventory()

            print("\nНажмите Enter, чтобы начать бой...")
            scanner.nextLine()
            scanner.nextLine()

            val won = InteractiveArena(player, enemy, scanner).fight()
            totalBattles++

            if (!won) {
                println("\n💀 ВЫ ПРОИГРАЛИ! Игра окончена.")
                println("   Всего боёв: $totalBattles")
                break
            }

            val reward = when (enemy) {
                is Warrior -> 100
                is Goblin -> 80
                is Mage -> 50
                else -> 50
            }
            player.addGold(reward)
            println("\n💰 НАГРАДА ЗА ПОБЕДУ: $reward золота!")
            println("   Ваше золото: ${player.gold}")

            println("\n🏆 ПОБЕДА! Что дальше?")
            println("    1 — ⚔️ Продолжить (новый бой)")
            println("    2 — 🚪 Закончить игру")
            print("  Ваш выбор: ")

            when (readInt(scanner)) {
                1 -> {
                    val savedGold = player.gold
                    player = createPlayer(playerChoice)
                    player.addGold(savedGold - 100)
                    player.restoreForNewBattle()
                }
                2 -> {
                    println("\n👋 Спасибо за игру! Всего боёв: $totalBattles")
                    continueGame = false
                }
                else -> {
                    println("❌ Неверный выбор. Завершение игры.")
                    continueGame = false
                }
            }
        }

    } catch (e: Exception) {
        println("\n❌ ОШИБКА: ${e.message}")
    }
}

// ============================================================
// ВСПОМОГАТЕЛЬНАЯ ФУНКЦИЯ
// ============================================================
fun readInt(scanner: Scanner): Int {
    return try {
        val input = scanner.next()
        input.toIntOrNull() ?: -1
    } catch (e: Exception) {
        -1
    }
}
