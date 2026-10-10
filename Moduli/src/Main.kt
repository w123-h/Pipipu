// ============================================================
// 1. БАЗОВЫЙ ИНТЕРФЕЙС ГРУЗА
// ============================================================
interface SpaceCargo {
    val id: String
    val sizeTerapets: Double
    fun EmergencyDump()
}

// ============================================================
// 2. КАТЕГОРИИ ГРУЗОВ (data class)
// ============================================================
data class Biomaterial(
    override val id: String,
    override val sizeTerapets: Double,
    val speciesName: String,
    val temperatureCelsius: Double
) : SpaceCargo {
    override fun EmergencyDump() {
        println("Внимание: Биоматериал $speciesName уничтожен!")
    }
}

data class CulturalHeritage(
    override val id: String,
    override val sizeTerapets: Double,
    val author: String,
    val yearCreated: Int
) : SpaceCargo {
    override fun EmergencyDump() {
        println("Потерян культурный объект: $id.")
    }
}

data class TechnicalBlueprint(
    override val id: String,
    override val sizeTerapets: Double,
    val systemComponent: String,
    val securityLevel: Int
) : SpaceCargo {
    init {
        require(securityLevel in 1..5) {
            "Уровень секретности должен быть от 1 до 5, получено: $securityLevel"
        }
    }

    override fun EmergencyDump() {
        println("СВЕРХСЕКРЕТНО: Чертеж $systemComponent стерт!")
    }
}

// ============================================================
// 3. ОБОБЩЁННЫЙ МОДУЛЬ ХРАНЕНИЯ
// ============================================================
class QuantumCapsule<T : SpaceCargo>(
    val maxCapacity: Double
) {
    private val cargoList = mutableListOf<T>()

    val currentLoad: Double
        get() = cargoList.sumOf { it.sizeTerapets }

    val itemsCount: Int
        get() = cargoList.size

    fun loadCargo(item: T): Boolean {
        val newTotal = currentLoad + item.sizeTerapets
        if (newTotal > maxCapacity) {
            println("  ❌ Ошибка: превышение лимита! " +
                    "Запрошено: ${"%.2f".format(newTotal)} / " +
                    "Доступно: ${"%.2f".format(maxCapacity)} терапет.")
            return false
        }
        cargoList.add(item)
        println("  ✅ Загружено: ${item.id} (${item.sizeTerapets} TP). " +
                "Загрузка: ${"%.2f".format(currentLoad)}/${maxCapacity} TP")
        return true
    }

    fun unloadCargo(id: String): T? {
        val index = cargoList.indexOfFirst { it.id == id }
        if (index == -1) {
            println("  ❌ Груз с ID '$id' не найден.")
            return null
        }
        val removed = cargoList.removeAt(index)
        println("  📦 Выгружено: ${removed.id}")
        return removed
    }

    fun triggerSolarFlareEmergency() {
        println("\n  ☀️ ВСПЫШКА НА СОЛНЦЕ! Экстренное уничтожение груза...")
        if (cargoList.isEmpty()) {
            println("  (капсула пуста)")
        } else {
            cargoList.forEach { it.EmergencyDump() }
        }
        cargoList.clear()
        println("  🧹 Капсула очищена. Загрузка: 0.0/${maxCapacity} TP\n")
    }
}

// ============================================================
// ТЕСТИРОВАНИЕ
// ============================================================
fun main() {
    println("=".repeat(60))
    println("🚀 СИСТЕМА УПРАВЛЕНИЯ БИО-КРИСТАЛЛИЧЕСКОЙ ПАМЯТЬЮ")
    println("=".repeat(60))

    // ------------------------------------------------------------
    // ТЕСТ 1: QuantumCapsule<Biomaterial> ёмкостью 10.0
    // ------------------------------------------------------------
    println("\n📋 ТЕСТ 1: Био-капсулы (лимит 10.0 TP)")
    println("-".repeat(60))

    val bioCapsule = QuantumCapsule<Biomaterial>(maxCapacity = 10.0)

    val bio1 = Biomaterial(
        id = "BIO-001",
        sizeTerapets = 4.0,
        speciesName = "Альфа-Центавра-Флора",
        temperatureCelsius = -50.0
    )
    val bio2 = Biomaterial(
        id = "BIO-002",
        sizeTerapets = 5.0,
        speciesName = "Земная пшеница",
        temperatureCelsius = 20.0
    )
    val bio3 = Biomaterial(
        id = "BIO-003",
        sizeTerapets = 2.0,
        speciesName = "Марсианский мох",
        temperatureCelsius = -10.0
    )

    bioCapsule.loadCargo(bio1)
    bioCapsule.loadCargo(bio2)
    bioCapsule.loadCargo(bio3)   // ← не поместится

    println("\n  Итого в капсуле: ${bioCapsule.itemsCount} объектов, " +
            "${bioCapsule.currentLoad}/10.0 TP")

    // ------------------------------------------------------------
    // ТЕСТ 2: QuantumCapsule<TechnicalBlueprint> + вспышка
    // ------------------------------------------------------------
    println("\n" + "=".repeat(60))
    println("📋 ТЕСТ 2: Технические чертежи + вспышка на солнце")
    println("-".repeat(60))

    val blueprintCapsule = QuantumCapsule<TechnicalBlueprint>(maxCapacity = 5.0)

    val blueprint = TechnicalBlueprint(
        id = "BP-001",
        sizeTerapets = 1.5,
        systemComponent = "Двигатель варп-ядра",
        securityLevel = 5
    )

    blueprintCapsule.loadCargo(blueprint)

    blueprintCapsule.triggerSolarFlareEmergency()

    println("  Итого в капсуле: ${blueprintCapsule.itemsCount} объектов, " +
            "${blueprintCapsule.currentLoad}/5.0 TP")

    // ------------------------------------------------------------
    // БОНУС: Культурное наследие + выгрузка
    // ------------------------------------------------------------
    println("\n" + "=".repeat(60))
    println("📋 БОНУС: Культурное наследие + выгрузка")
    println("-".repeat(60))

    val heritageCapsule = QuantumCapsule<CulturalHeritage>(maxCapacity = 8.0)

    heritageCapsule.loadCargo(
        CulturalHeritage("CH-001", 3.0, "Леонардо да Винчи", 1503)
    )
    heritageCapsule.loadCargo(
        CulturalHeritage("CH-002", 4.0, "Моцарт", 1788)
    )

    val unloaded = heritageCapsule.unloadCargo("CH-001")
    println("  Возвращённый объект: $unloaded")

    println("\n" + "=".repeat(60))
    println("✅ СИМУЛЯЦИЯ ЗАВЕРШЕНА")
    println("=".repeat(60))
}