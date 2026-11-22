package com.yourpackage.dungeoncrawler

enum class ItemRarity(val color: Int, val multiplier: Float) {
    COMMON(0xFFCCCCCC.toInt(), 1.0f), UNCOMMON(0xFF69F0AE.toInt(), 1.3f),
    RARE(0xFF42A5F5.toInt(), 1.6f), EPIC(0xFFAB47BC.toInt(), 2.0f), LEGENDARY(0xFFFFA726.toInt(), 2.5f)
}

enum class ItemType { WEAPON, ARMOR, POTION, SCROLL }

data class Item(
    val id: String, val name: String, val type: ItemType, val rarity: ItemRarity,
    val stats: Map<String, Int>, val description: String, val value: Int
) {
    companion object {
        fun generateWeapon(level: Int): Item {
            val rarity = rollRarity(level)
            val name = "${listOf("Rusty","Fine","Enchanted","Infernal","Divine").random()} ${listOf("Sword","Axe","Mace","Dagger").random()}"
            val damage = ((10 + level * 3) * rarity.multiplier).toInt()
            return Item("weapon_${System.nanoTime()}", name, ItemType.WEAPON, rarity, mapOf("damage" to damage), "Deals $damage damage", damage * 5)
        }
        fun generateArmor(level: Int): Item {
            val rarity = rollRarity(level)
            val name = "${listOf("Leather","Sturdy","Mithril","Infernal","Divine").random()} ${listOf("Shield","Chestplate","Helm","Boots").random()}"
            val defense = ((5 + level * 2) * rarity.multiplier).toInt()
            return Item("armor_${System.nanoTime()}", name, ItemType.ARMOR, rarity, mapOf("defense" to defense), "Reduces damage by $defense", defense * 4)
        }
        fun generatePotion(): Item = Item("potion_${System.nanoTime()}", "Health Potion", ItemType.POTION, ItemRarity.COMMON, mapOf("health" to 50), "Restores 50 HP", 25)
        private fun rollRarity(level: Int): ItemRarity {
            val roll = kotlin.random.Random.nextDouble()
            return when { roll < 0.01 + level * 0.002 -> ItemRarity.LEGENDARY; roll < 0.06 + level * 0.007 -> ItemRarity.EPIC
        roll < 0.21 + level * 0.017 -> ItemRarity.RARE; roll < 0.61 -> ItemRarity.UNCOMMON; else -> ItemRarity.COMMON }
        }
    }
}

data class Inventory(
    val weapons: MutableList<Item> = mutableListOf(), val armors: MutableList<Item> = mutableListOf(),
    val consumables: MutableList<Item> = mutableListOf(), var equippedWeapon: Item? = null, var equippedArmor: Item? = null
) {
    fun addItem(item: Item) { when (item.type) { ItemType.WEAPON -> weapons.add(item); ItemType.ARMOR -> armors.add(item); else -> consumables.add(item) } }
    fun removeItem(item: Item) { when (item.type) { ItemType.WEAPON -> weapons.remove(item); ItemType.ARMOR -> armors.remove(item); else -> consumables.remove(item) } }
    fun getAttackBonus(): Int = equippedWeapon?.stats?.get("damage") ?: 0
    fun getDefenseBonus(): Int = equippedArmor?.stats?.get("defense") ?: 0
}
