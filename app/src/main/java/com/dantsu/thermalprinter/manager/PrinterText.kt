package com.dantsu.thermalprinter.manager

import android.graphics.Bitmap
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.textparser.PrinterTextParserImg

enum class Alignment(val value: String) {
    LEFT("[L]"),
    CENTER("[C]"),
    RIGHT("[R]")
}

enum class FontSize(val value: String) {
    NORMAL("normal"),
    WIDE("wide"),
    TALL("tall"),
    BIG("big"),
    BIG2("big-2"),
    BIG3("big-3"),
    BIG4("big-4"),
    BIG5("big-5"),
    BIG6("big-6"),
}

class PrinterText {
    private var formattedText = ""

    fun row(vararg text: String): PrinterText {
        formattedText += text.asList().joinToString(" ") { it }
        formattedText += "\n"
        return this
    }

    fun row2(textLeft: String, textRight: String): PrinterText {
        formattedText += textRow(textLeft, align = Alignment.LEFT) + textRow(
            textRight,
            align = Alignment.RIGHT
        )
        formattedText += "\n"
        return this
    }

    fun text(
        text: String,
        align: Alignment = Alignment.CENTER,
        fontSize: FontSize = FontSize.NORMAL,
        bold: Boolean = false
    ): PrinterText {
        val alignValue = align.value
        val fontSizeValue = fontSize.value
        val result = "$alignValue<font size='$fontSizeValue'>$text</font>"
        formattedText += "$result\n"
        return this
    }

    fun textRow(
        text: String,
        align: Alignment = Alignment.CENTER,
        fontSize: FontSize = FontSize.NORMAL,
        bold: Boolean = false
    ): String {
        val alignValue = align.value
        val fontSizeValue = fontSize.value
        return "$alignValue<font size='$fontSizeValue'>$text</font>"
    }

    fun newLine(): PrinterText {
        formattedText += "[L]\n"
        return this
    }

    fun divider(): PrinterText {
        text("-".repeat(48))
        return this
    }

    fun image(printer: EscPosPrinter, bitmap: Bitmap): PrinterText {
        val image = PrinterTextParserImg.bitmapToHexadecimalString(printer, bitmap)
        formattedText += "[C]<img>$image</img>\n"
        return this
    }

    override fun toString(): String {
        return formattedText
    }
}