package apw.sec.android.gallery.data

import android.content.Context
import org.json.JSONObject
import org.mozilla.javascript.Context as RhinoContext
import org.mozilla.javascript.Scriptable
import java.io.InputStreamReader

class RandomQuotes(private val context: Context) {

    fun getRandomQuote(): Quotes {
        val jsResult = runJavaScriptFromAssets(context, "quotes.js")
        return parseJsonToQuote(jsResult)
    }

    private fun runJavaScriptFromAssets(context: Context, fileName: String): String {
        val inputStream = context.assets.open(fileName)
        val script = InputStreamReader(inputStream).readText()

        val rhino = RhinoContext.enter()
        return try {
            rhino.optimizationLevel = -1
            val scope: Scriptable = rhino.initStandardObjects()
            rhino.evaluateString(scope, script, "JavaScript", 1, null)

            val function = scope.get("getRandomQuote", scope)
            if (function is org.mozilla.javascript.Function) {
                function.call(rhino, scope, scope, arrayOf()).toString()
            } else {
                """{"text": "Error: Function not found", "author": "System"}"""
            }
        } finally {
            RhinoContext.exit()
        }
    }

    private fun parseJsonToQuote(jsonString: String): Quotes {
        val jsonObject = JSONObject(jsonString)
        val quote = jsonObject.getString("text")
        val author = jsonObject.getString("author")
        return Quotes(quote, author)
    }
}