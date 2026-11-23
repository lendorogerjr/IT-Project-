package routeberekening.invoer

object Json {
    fun lees(tekst: String): Any? {
        val p = Lezer(tekst)
        val waarde = p.waarde()
        p.spaties()
        require(p.klaar()) { "Onverwachte tekst na het einde van de JSON op positie ${p.pos}" }
        return waarde
    }

    private class Lezer(val s: String) {
        var pos = 0
        fun klaar() = pos >= s.length
        fun spaties() { while (pos < s.length && s[pos].isWhitespace()) pos++ }
        private fun fout(msg: String): Nothing = throw IllegalArgumentException("JSON-fout op positie $pos: $msg")

        fun waarde(): Any? {
            spaties()
            if (klaar()) fout("onverwacht einde")
            return when (val c = s[pos]) {
                '{' -> obj()
                '[' -> lijst()
                '"' -> tekst()
                't' -> woord("true", true)
                'f' -> woord("false", false)
                'n' -> woord("null", null)
                else -> if (c == '-' || c.isDigit()) getal() else fout("onverwacht teken '$c'")
            }
        }

        private fun woord(w: String, v: Any?): Any? {
            if (!s.startsWith(w, pos)) fout("verwachtte $w")
            pos += w.length
            return v
        }

        private fun getal(): Double {
            val begin = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] in "+-.eE")) pos++
            return s.substring(begin, pos).toDoubleOrNull() ?: fout("ongeldig getal")
        }

        private fun tekst(): String {
            pos++
            val sb = StringBuilder()
            while (true) {
                if (klaar()) fout("tekst niet afgesloten")
                val c = s[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (klaar()) fout("onvolledige escape")
                        when (val e = s[pos++]) {
                            'n' -> sb.append('\n'); 't' -> sb.append('\t'); 'r' -> sb.append('\r')
                            'b' -> sb.append('\b'); 'f' -> sb.append('\u000c')
                            'u' -> {
                                if (pos + 4 > s.length) fout("onvolledige \\u")
                                sb.append(s.substring(pos, pos + 4).toInt(16).toChar()); pos += 4
                            }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun lijst(): List<Any?> {
            pos++
            val uit = mutableListOf<Any?>()
            spaties()
            if (pos < s.length && s[pos] == ']') { pos++; return uit }
            while (true) {
                uit += waarde()
                spaties()
                if (klaar()) fout("lijst niet afgesloten")
                when (s[pos++]) { ',' -> {} ; ']' -> return uit; else -> fout("verwachtte , of ]") }
            }
        }

        private fun obj(): Map<String, Any?> {
            pos++
            val uit = LinkedHashMap<String, Any?>()
            spaties()
            if (pos < s.length && s[pos] == '}') { pos++; return uit }
            while (true) {
                spaties()
                if (klaar() || s[pos] != '"') fout("verwachtte een sleutel")
                val sleutel = tekst()
                spaties()
                if (klaar() || s[pos++] != ':') fout("verwachtte :")
                uit[sleutel] = waarde()
                spaties()
                if (klaar()) fout("object niet afgesloten")
                when (s[pos++]) { ',' -> {} ; '}' -> return uit; else -> fout("verwachtte , of }") }
            }
        }
    }
}
