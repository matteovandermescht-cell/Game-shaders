package com.gameshaders.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var screenshotPreview: ImageView
    private lateinit var screenshotLabel: TextView
    private lateinit var recognitionInput: EditText
    private lateinit var actionInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var examplesContainer: LinearLayout
    private var selectedScreenshot: Uri? = null
    private val prefs by lazy { getSharedPreferences("gameplay_training", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(12, 14, 22)
        window.navigationBarColor = Color.rgb(12, 14, 22)
        buildUi()
        renderExamples()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(Color.rgb(12, 14, 22))
        }
        val scroll = ScrollView(this)
        scroll.addView(root)
        root.addView(text("GAME SHADERS · AI TRAINING", 12f, Color.rgb(111, 170, 255), true))
        root.addView(text("Teach by example", 29f, Color.WHITE, true).apply {
            setPadding(0, dp(8), 0, dp(5))
        })
        root.addView(text("Add gameplay screenshots and explain what the AI should notice and do next.", 14f, Color.rgb(174, 182, 201), false))

        val card = panel()
        root.addView(card, fullWidth().apply { topMargin = dp(20) })
        card.addView(text("1 · GAMEPLAY SCREENSHOT", 12f, Color.rgb(111, 170, 255), true))
        screenshotPreview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.rgb(17, 20, 31))
            visibility = ImageView.GONE
        }
        card.addView(screenshotPreview, LinearLayout.LayoutParams(-1, dp(170)).apply { topMargin = dp(10) })
        screenshotLabel = text("No screenshot selected", 13f, Color.rgb(174, 182, 201), false)
        card.addView(screenshotLabel)
        card.addView(button("Choose screenshot") { chooseScreenshot() }, fullWidth())

        card.addView(text("2 · WHAT SHOULD THE AI NOTICE?", 12f, Color.rgb(111, 170, 255), true).apply {
            setPadding(0, dp(18), 0, dp(8))
        })
        recognitionInput = edit("Example: The player is near the doorway; an opponent is on the left.")
        card.addView(recognitionInput, fullWidth())

        card.addView(text("3 · WHAT SHOULD HAPPEN NEXT?", 12f, Color.rgb(111, 170, 255), true).apply {
            setPadding(0, dp(16), 0, dp(8))
        })
        actionInput = edit("Example: Move left, then tap the interact button.")
        card.addView(actionInput, fullWidth())

        card.addView(text("Extra instructions (optional)", 12f, Color.rgb(174, 182, 201), false).apply {
            setPadding(0, dp(14), 0, dp(6))
        })
        notesInput = edit("Timing, conditions, or what to avoid…")
        card.addView(notesInput, fullWidth())
        card.addView(button("Save training example") { saveExample() }, fullWidth().apply { topMargin = dp(16) })

        root.addView(text("Saved examples", 20f, Color.WHITE, true).apply {
            setPadding(0, dp(24), 0, dp(10))
        })
        root.addView(text("These examples stay on this device. More varied examples help define the behavior you want, but saving examples alone does not train an AI model yet.", 13f, Color.rgb(174, 182, 201), false))
        examplesContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(examplesContainer, fullWidth())

        root.addView(button("Export training examples") { exportExamples() }, fullWidth().apply { topMargin = dp(14) })
        root.addView(text("Next development step: connect these examples to a vision model and a user-approved action controller. Screen capture and automatic taps require explicit Android permissions.", 12f, Color.rgb(130, 139, 160), false).apply {
            setPadding(0, dp(16), 0, 0)
        })
        setContentView(scroll)
    }

    private fun chooseScreenshot() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_SCREENSHOT)
    }

    @Deprecated("Using the platform document picker to avoid extra dependencies")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SCREENSHOT && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) { }
            selectedScreenshot = uri
            screenshotPreview.setImageURI(uri)
            screenshotPreview.visibility = ImageView.VISIBLE
            screenshotLabel.text = "Screenshot selected"
        }
    }

    private fun saveExample() {
        val image = selectedScreenshot
        val recognition = recognitionInput.text.toString().trim()
        val action = actionInput.text.toString().trim()
        if (image == null) {
            Toast.makeText(this, "Choose a gameplay screenshot first.", Toast.LENGTH_SHORT).show()
            return
        }
        if (recognition.isBlank() || action.isBlank()) {
            Toast.makeText(this, "Describe what to notice and what should happen next.", Toast.LENGTH_LONG).show()
            return
        }
        val examples = readExamples()
        val item = JSONObject().apply {
            put("id", System.currentTimeMillis().toString())
            put("imageUri", image.toString())
            put("recognition", recognition)
            put("action", action)
            put("notes", notesInput.text.toString().trim())
            put("createdAt", System.currentTimeMillis())
        }
        examples.put(item)
        prefs.edit().putString(KEY_EXAMPLES, examples.toString()).apply()
        recognitionInput.text.clear()
        actionInput.text.clear()
        notesInput.text.clear()
        selectedScreenshot = null
        screenshotPreview.setImageDrawable(null)
        screenshotPreview.visibility = ImageView.GONE
        screenshotLabel.text = "No screenshot selected"
        renderExamples()
        Toast.makeText(this, "Training example saved on this device.", Toast.LENGTH_SHORT).show()
    }

    private fun renderExamples() {
        if (!::examplesContainer.isInitialized) return
        examplesContainer.removeAllViews()
        val examples = readExamples()
        if (examples.length() == 0) {
            examplesContainer.addView(panel().apply {
                addView(text("No examples yet. Add your first screenshot above.", 14f, Color.rgb(174, 182, 201), false))
            }, fullWidth().apply { topMargin = dp(10) })
            return
        }
        for (i in examples.length() - 1 downTo 0) {
            val item = examples.optJSONObject(i) ?: continue
            val card = panel()
            card.addView(text("EXAMPLE ${i + 1}", 11f, Color.rgb(111, 170, 255), true))
            val preview = ImageView(this).apply {
                setBackgroundColor(Color.rgb(17, 20, 31))
                scaleType = ImageView.ScaleType.CENTER_CROP
                try { setImageURI(Uri.parse(item.optString("imageUri"))) } catch (_: Exception) { }
            }
            card.addView(preview, LinearLayout.LayoutParams(-1, dp(110)).apply { topMargin = dp(8) })
            card.addView(text("Notice: ${item.optString("recognition")}", 14f, Color.WHITE, true).apply { setPadding(0, dp(8), 0, dp(4)) })
            card.addView(text("Next action: ${item.optString("action")}", 13f, Color.rgb(174, 182, 201), false))
            val notes = item.optString("notes")
            if (notes.isNotBlank()) card.addView(text("Notes: $notes", 12f, Color.rgb(145, 154, 175), false).apply { setPadding(0, dp(5), 0, 0) })
            card.addView(button("Delete example") { deleteExample(item.optString("id")) }, fullWidth().apply { topMargin = dp(8) })
            examplesContainer.addView(card, fullWidth().apply { topMargin = dp(10) })
        }
    }

    private fun deleteExample(id: String) {
        val old = readExamples()
        val updated = JSONArray()
        for (i in 0 until old.length()) {
            val item = old.optJSONObject(i) ?: continue
            if (item.optString("id") != id) updated.put(item)
        }
        prefs.edit().putString(KEY_EXAMPLES, updated.toString()).apply()
        renderExamples()
    }

    private fun exportExamples() {
        val json = readExamples().toString(2)
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "gameplay-training-examples.json")
        }
        pendingExport = json
        startActivityForResult(intent, REQUEST_EXPORT)
    }

    private var pendingExport: String? = null

    private fun handleExportResult(data: Intent?) {
        val uri = data?.data ?: return
        val content = pendingExport ?: return
        try {
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
            Toast.makeText(this, "Export complete.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not export: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            pendingExport = null
        }
    }

    private fun readExamples(): JSONArray = try {
        JSONArray(prefs.getString(KEY_EXAMPLES, "[]") ?: "[]")
    } catch (_: Exception) { JSONArray() }

    private fun panel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(14), dp(14), dp(14))
        setBackgroundColor(Color.rgb(25, 29, 43))
    }

    private fun text(value: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun edit(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 14f
        setTextColor(Color.WHITE)
        setHintTextColor(Color.rgb(115, 124, 146))
        setPadding(dp(12), dp(10), dp(12), dp(10))
        setBackgroundColor(Color.rgb(17, 20, 31))
        minLines = 2
        maxLines = 4
    }

    private fun button(title: String, action: () -> Unit) = Button(this).apply {
        text = title
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun fullWidth() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_SCREENSHOT = 2001
        private const val REQUEST_EXPORT = 2002
        private const val KEY_EXAMPLES = "examples"
    }
}