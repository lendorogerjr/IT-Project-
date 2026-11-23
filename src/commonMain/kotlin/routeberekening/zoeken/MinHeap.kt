package routeberekening.zoeken

class MinHeap {
    private class Item(val sleutel: String, val kost: Double, val volgorde: Long)

    private val items = ArrayList<Item>()
    private var teller = 0L

    fun isEmpty() = items.isEmpty()

    private fun kleiner(a: Item, b: Item) =
        a.kost < b.kost || (a.kost == b.kost && a.volgorde < b.volgorde)

    private fun wissel(i: Int, j: Int) {
        val tijdelijk = items[i]
        items[i] = items[j]
        items[j] = tijdelijk
    }

    fun add(sleutel: String, kost: Double) {
        items.add(Item(sleutel, kost, teller++))
        var i = items.size - 1
        while (i > 0) {
            val ouder = (i - 1) / 2
            if (!kleiner(items[i], items[ouder])) break
            wissel(i, ouder)
            i = ouder
        }
    }

    fun pop(): Pair<String, Double> {
        val top = items[0]
        val laatste = items.removeAt(items.size - 1)
        if (items.isNotEmpty()) {
            items[0] = laatste
            var i = 0
            while (true) {
                val links = 2 * i + 1
                val rechts = links + 1
                var kleinste = i
                if (links < items.size && kleiner(items[links], items[kleinste])) kleinste = links
                if (rechts < items.size && kleiner(items[rechts], items[kleinste])) kleinste = rechts
                if (kleinste == i) break
                wissel(i, kleinste)
                i = kleinste
            }
        }
        return top.sleutel to top.kost
    }
}
