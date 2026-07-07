package ru.kkalscan.app.platform

import kotlinx.browser.document
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

internal fun bindWasmPhotoInput(onPhotoPicked: (ByteArray?) -> Unit): () -> Unit {
    val input = document.getElementById("wasm-photo-input") as HTMLInputElement
    input.onchange = {
        val file = input.files?.item(0)
        if (file == null) {
            onPhotoPicked(null)
        } else {
            val reader = FileReader()
            reader.onload = {
                val buffer = reader.result as ArrayBuffer
                val view = Int8Array(buffer)
                onPhotoPicked(ByteArray(view.length) { i -> view[i] })
            }
            reader.readAsArrayBuffer(file)
        }
        input.value = ""
        null
    }
    return {
        input.value = ""
        input.click()
    }
}
