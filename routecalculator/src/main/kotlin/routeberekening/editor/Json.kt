package routeberekening.editor

import kotlin.math.abs

/** Kleine JSON-lezer en -schrijver, zodat er geen extra bibliotheek nodig is. */
object Json {

    fun schrijf(waarde: Any?): String {
        val sb = StringBuilder()
        schrijf(waarde, sb, 0)
        return sb.append('\n').toString()
    }

    private fun schrijf(v: Any?, sb: StringBuilder, diepte: Int) {
        when (v) {
            null -> sb.append("null")
            is String -> tekst(v, sb)
            is Boolean, is Int, is Long -> sb.append(v.toString())
            is Double -> sb.append(getal(v))
            is Number -> sb.append(v.toString())
            is Map<*, *> -> {
                val inspringen = diepte == 0
                sb.append('{')
                v.entries.forEachIndexed { i, (k, w) ->
                    if (i > 0) sb.append(',')
                    if (inspringen) sb.append("\n  ")
                    tekst(k.toString(), sb)
                    sb.append(if (inspringen) ": " else ":")
                    schrijf(w, sb, diepte + 1)
                }
                if (inspringen) sb.append('\n')
                sb.append('}')
            }
            is Iterable<*> -> {
                val lijst = v.toList()
                val inspringen = diepte == 1 && lijst.any { it is Map<*, *> }
                sb.append('[')
                lijst.forEachIndexed { i, w ->
                    if (i > 0) sb.append(',')
                    if (inspringen) sb.append("\n    ")
                    schrijf(w, sb, diepte + 1)
                }
                if (inspringen) sb.append("\n  ")
                sb.append(']')
            }
            else -> tekst(v.toString(), sb)
        }
    }

    private fun getal(d: Double) =
        if (d == Math.rint(d) && abs(d) < 1e15) d.toLong().toString() else d.toString()

    private fun tekst(s: String, sb: StringBuilder) {
        sb.append('"')
        for (c in s) when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> if (c < ' ') sb.append(String.format("\\u%04x", c.code)) else sb.append(c)
        }
        sb.append('"')
    }

    fun lees(s: String): Any? {
        val lezer = Lezer(s)
        try {
            val v = lezer.waarde()
            lezer.spaties()
            if (lezer.i != s.length) lezer.fout("onverwachte tekst")
            return v
        } catch (e: IndexOutOfBoundsException) {
            lezer.fout("onverwacht einde van het bestand")
        }
    }

    private class Lezer(val s: String) {
        var i = 0

        fun fout(m: String): Nothing = throw IllegalArgumentException("JSON-fout op positie $i: $m")

        fun spaties() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        fun waarde(): Any? {
            spaties()
            return when (s[i]) {
                '{' -> object_()
                '[' -> lijst()
                '"' -> tekst()
                't' -> woord("true", true)
                'f' -> woord("false", false)
                'n' -> woord("null", null)
                else -> getal()
            }
        }

        fun woord(w: String, v: Any?): Any? {
            if (!s.startsWith(w, i)) fout("verwacht $w")
            i += w.length
            return v
        }

        fun getal(): Double {
            val begin = i
            while (i < s.length && (s[i].isDigit() || s[i] in "+-.eE")) i++
            return s.substring(begin, i).toDoubleOrNull() ?: fout("ongeldig getal")
        }

        fun tekst(): String {
            i++
            val sb = StringBuilder()
            while (true) {
                when (val c = s[i++]) {
                    '"' -> return sb.toString()
                    '\\' -> when (val e = s[i++]) {
                        'n' -> sb.append('\n')
                        't' -> sb.append('\t')
                        'r' -> sb.append('\r')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'u' -> { sb.append(s.substring(i, i + 4).toInt(16).toChar()); i += 4 }
                        else -> sb.append(e)
                    }
                    else -> sb.append(c)
                }
            }
        }

        fun object_(): Map<String, Any?> {
            i++
            val m = LinkedHashMap<String, Any?>()
            spaties()
            if (s[i] == '}') { i++; return m }
            while (true) {
                spaties()
                if (s[i] != '"') fout("verwacht een sleutel")
                val k = tekst()
                spaties()
                if (s[i] != ':') fout("verwacht :")
                i++
                m[k] = waarde()
                spaties()
                when (s[i++]) {
                    ',' -> continue
                    '}' -> return m
                    else -> fout("verwacht , of }")
                }
            }
        }

        fun lijst(): List<Any?> {
            i++
            val l = ArrayList<Any?>()
            spaties()
            if (s[i] == ']') { i++; return l }
            while (true) {
                l += waarde()
                spaties()
                when (s[i++]) {
                    ',' -> continue
                    ']' -> return l
                    else -> fout("verwacht , of ]")
                }
            }
        }
    }
}
