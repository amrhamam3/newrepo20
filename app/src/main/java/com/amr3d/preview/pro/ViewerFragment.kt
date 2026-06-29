package com.amr3d.preview.pro

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.InputType
import android.view.*
import android.view.animation.*
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class ViewerFragment : Fragment() {

    // ═══ Views ═══
    private lateinit var glViewerView: GLViewerView
    private lateinit var emptyStateText: TextView
    private lateinit var welcomeText: TextView
    private lateinit var welcomeOverlay: TextView
    private lateinit var btnOpenFile: Button
    private lateinit var btnWhatsapp: ImageButton
    private lateinit var btnMeasureTool: ToggleButton
    private lateinit var btnInspect: Button
    private lateinit var btnResetView: Button
    private lateinit var btnWireframe: ToggleButton
    private lateinit var btnMaterial: Button
    private lateinit var btnUnit: Button
    private lateinit var btnExport: Button
    private lateinit var btnLightToggle: ToggleButton
    private lateinit var btnDirections: Button
    private lateinit var directionsPanel: View
    private lateinit var btnViewFront: Button
    private lateinit var btnViewBack: Button
    private lateinit var btnViewLeft: Button
    private lateinit var btnViewRight: Button
    private lateinit var btnViewTop: Button
    private lateinit var btnViewBottom: Button
    private lateinit var measurementCard: CardView
    private lateinit var measurementText: TextView
    private lateinit var inspectionCard: CardView
    private lateinit var inspectionText: TextView
    private lateinit var lightWheelContainer: ViewGroup
    private lateinit var lightWheel: SemiCircleLightView
    private lateinit var btnCloseLightWheel: ImageButton
    private lateinit var loadingContainer: View
    private lateinit var loadingProgress: ProgressBar
    private lateinit var loadingText: TextView

    private var currentModel: STLModel? = null
    private var measureModeOn = false
    private var currentUnit = MeasurementUnit.MM

    // ملف معلّق من MainActivity (قبل init الـ View)
    private var pendingUri: Uri? = null

    private val openDocumentLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}
            loadFile(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_viewer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupWelcome()
        wireUpListeners()
        animateToolbarEntrance()

        // تحميل أي ملف كان معلقاً قبل init الـ View
        pendingUri?.let {
            pendingUri = null
            loadFile(it)
        }
    }

    private fun bindViews(v: View) {
        glViewerView        = v.findViewById(R.id.glViewerView)
        emptyStateText      = v.findViewById(R.id.emptyStateText)
        welcomeText         = v.findViewById(R.id.welcomeText)
        welcomeOverlay      = v.findViewById(R.id.welcomeOverlay)
        btnOpenFile         = v.findViewById(R.id.btnOpenFile)
        btnWhatsapp         = v.findViewById(R.id.btnWhatsapp)
        btnMeasureTool      = v.findViewById(R.id.btnMeasureTool)
        btnInspect          = v.findViewById(R.id.btnInspect)
        btnResetView        = v.findViewById(R.id.btnResetView)
        btnWireframe        = v.findViewById(R.id.btnWireframe)
        btnMaterial         = v.findViewById(R.id.btnMaterial)
        btnUnit             = v.findViewById(R.id.btnUnit)
        btnExport           = v.findViewById(R.id.btnExport)
        btnLightToggle      = v.findViewById(R.id.btnLightToggle)
        btnDirections       = v.findViewById(R.id.btnDirections)
        directionsPanel     = v.findViewById(R.id.directionsPanel)
        btnViewFront        = v.findViewById(R.id.btnViewFront)
        btnViewBack         = v.findViewById(R.id.btnViewBack)
        btnViewLeft         = v.findViewById(R.id.btnViewLeft)
        btnViewRight        = v.findViewById(R.id.btnViewRight)
        btnViewTop          = v.findViewById(R.id.btnViewTop)
        btnViewBottom       = v.findViewById(R.id.btnViewBottom)
        measurementCard     = v.findViewById(R.id.measurementCard)
        measurementText     = v.findViewById(R.id.measurementText)
        inspectionCard      = v.findViewById(R.id.inspectionCard)
        inspectionText      = v.findViewById(R.id.inspectionText)
        lightWheelContainer = v.findViewById(R.id.lightWheelContainer)
        lightWheel          = v.findViewById(R.id.lightWheel)
        btnCloseLightWheel  = v.findViewById(R.id.btnCloseLightWheel)
        loadingContainer    = v.findViewById(R.id.loadingContainer)
        loadingProgress     = v.findViewById(R.id.loadingProgress)
        loadingText         = v.findViewById(R.id.loadingText)
    }

    private fun setupWelcome() {
        val ctx = requireContext()
        val savedName = MainActivity.getUserName(ctx)
        if (savedName.isEmpty()) {
            showNameDialog(ctx)
        } else {
            welcomeText.text = "👋  مرحباً، $savedName"
            welcomeText.visibility = View.VISIBLE
        }
    }

    private fun showNameDialog(ctx: Context) {
        val input = EditText(ctx).apply {
            hint = "اكتب اسمك هنا"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
            setTextColor(0xFFF2F3F5.toInt())
            setHintTextColor(0xFF9CA3AF.toInt())
            setPadding(40, 24, 40, 24)
            textSize = 16f
        }
        AlertDialog.Builder(ctx)
            .setTitle("🎮  مرحباً بك في Amr3D Preview")
            .setMessage("ما اسمك؟")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("ابدأ") { _, _ ->
                val name = input.text.toString().trim().ifEmpty { "صديقي" }
                MainActivity.saveUserName(ctx, name)
                welcomeText.text = "👋  مرحباً، $name"
                welcomeText.visibility = View.VISIBLE
                // Overlay ترحيبي
                welcomeOverlay.text = "🎮  أهلاً وسهلاً\n$name"
                welcomeOverlay.visibility = View.VISIBLE
                welcomeOverlay.alpha = 0f
                welcomeOverlay.animate().alpha(1f).setDuration(400).withEndAction {
                    welcomeOverlay.animate().alpha(0f).setStartDelay(1800).setDuration(500)
                        .withEndAction { welcomeOverlay.visibility = View.GONE }.start()
                }.start()
            }.show()
    }

    private fun animateToolbarEntrance() {
        listOf(
            view?.findViewById<View>(R.id.topBar),
            view?.findViewById<View>(R.id.displayToolbar),
            view?.findViewById<View>(R.id.bottomToolbar)
        ).forEachIndexed { i, v ->
            v ?: return@forEachIndexed
            val dir = if (i == 0) -180f else 180f
            v.translationY = dir; v.alpha = 0f
            v.animate().translationY(0f).alpha(1f).setDuration(350)
                .setStartDelay(i * 80L).setInterpolator(DecelerateInterpolator(2f)).start()
        }
    }

    private fun wireUpListeners() {
        btnOpenFile.setOnClickListener { animBtn(it); openDocumentLauncher.launch(arrayOf("*/*")) }

        btnMeasureTool.setOnCheckedChangeListener { btn, isChecked ->
            animBtn(btn); measureModeOn = isChecked
            if (!isChecked) {
                glViewerView.stlRenderer.clearMeasurementPoints()
                measurementCard.visibility = View.GONE
            } else {
                inspectionCard.visibility = View.GONE
                Toast.makeText(context, "اضغط على نقطتين على الموديل", Toast.LENGTH_LONG).show()
            }
        }

        btnInspect.setOnClickListener {
            animBtn(it)
            currentModel?.let { m -> showInspectionReport(m) }
                ?: Toast.makeText(context, "افتح ملف أولاً", Toast.LENGTH_SHORT).show()
        }

        btnResetView.setOnClickListener  { animBtn(it); resetCamera() }
        btnWhatsapp.setOnClickListener   { animBtn(it); openWhatsapp() }

        btnWireframe.setOnCheckedChangeListener { btn, c ->
            animBtn(btn); glViewerView.stlRenderer.wireframeMode = c
        }

        btnMaterial.setOnClickListener  { animBtn(it); showMaterialGrid() }
        btnUnit.setOnClickListener      { animBtn(it); cycleUnit() }
        btnExport.setOnClickListener    { animBtn(it); exportCurrentView() }

        btnLightToggle.setOnCheckedChangeListener { btn, c ->
            animBtn(btn)
            lightWheelContainer.visibility = if (c) View.VISIBLE else View.GONE
        }
        btnCloseLightWheel.setOnClickListener {
            lightWheelContainer.visibility = View.GONE
            btnLightToggle.isChecked = false
        }
        lightWheel.onAngleChanged = { angle ->
            glViewerView.queueEvent { glViewerView.stlRenderer.lightAngle = angle }
        }

        // أزرار الاتجاهات الـ 6
        btnDirections.setOnClickListener {
            animBtn(it)
            val showing = directionsPanel.visibility == View.VISIBLE
            if (showing) {
                directionsPanel.animate().alpha(0f).setDuration(150)
                    .withEndAction { directionsPanel.visibility = View.GONE }.start()
            } else {
                directionsPanel.alpha = 0f
                directionsPanel.visibility = View.VISIBLE
                directionsPanel.animate().alpha(1f).setDuration(200).start()
            }
        }
        btnViewFront.setOnClickListener  { jumpToView(-10f, 0f);   hideDirections() }
        btnViewBack.setOnClickListener   { jumpToView(-10f, 180f); hideDirections() }
        btnViewLeft.setOnClickListener   { jumpToView(-10f, -90f); hideDirections() }
        btnViewRight.setOnClickListener  { jumpToView(-10f, 90f);  hideDirections() }
        btnViewTop.setOnClickListener    { jumpToView(-89f, 0f);   hideDirections() }
        btnViewBottom.setOnClickListener { jumpToView(89f, 0f);    hideDirections() }

        glViewerView.onSingleTap = { x, y -> if (measureModeOn) handleMeasurementTap(x, y) }

        inspectionCard.setOnClickListener  { inspectionCard.visibility = View.GONE }
        measurementCard.setOnClickListener {
            measurementCard.visibility = View.GONE
            glViewerView.stlRenderer.clearMeasurementPoints()
        }
    }

    private fun hideDirections() {
        directionsPanel.visibility = View.GONE
    }

    private fun animBtn(v: View) {
        v.animate().scaleX(0.87f).scaleY(0.87f).setDuration(70)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                v.animate().scaleX(1f).scaleY(1f).setDuration(140)
                    .setInterpolator(OvershootInterpolator(2.2f)).start()
            }.start()
    }

    // ══ تحميل الملف — آمن حتى قبل onViewCreated ══
    fun loadFile(uri: Uri) {
        // إذا لم يكن الـ View جاهزاً بعد، نحفظ الـ URI ونحمّله لاحقاً
        if (!isAdded || view == null) {
            pendingUri = uri
            return
        }

        showLoadingBar("جارٍ فتح الملف...", 0)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val model = withContext(Dispatchers.IO) {
                    STLParser.parse(requireContext(), uri)
                }

                if (!isAdded || view == null) return@launch

                updateLoadingBar("جارٍ التحضير للعرض...", 95)
                delay(200)
                updateLoadingBar("اكتمل ✓", 100)
                delay(300)
                hideLoadingBar()

                currentModel = model

                // رفع الموديل على GL thread
                glViewerView.queueEvent {
                    glViewerView.stlRenderer.setModel(model)
                }

                requireActivity().runOnUiThread {
                    emptyStateText.visibility  = View.GONE
                    welcomeText.visibility     = View.GONE
                    inspectionCard.visibility  = View.GONE
                    measurementCard.visibility = View.GONE
                    btnMeasureTool.isChecked   = false
                    btnWireframe.isChecked     = false
                    directionsPanel.visibility = View.GONE
                }

                // حفظ في التاريخ — المسار الحقيقي فقط
                saveToHistory(uri)

                Toast.makeText(context, "✅  ${model.triangleCount} مثلث", Toast.LENGTH_SHORT).show()

            } catch (e: SecurityException) {
                if (!isAdded || view == null) return@launch
                hideLoadingBar()
                Toast.makeText(context, "خطأ في الأذونات — حاول مرة أخرى", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                if (!isAdded || view == null) return@launch
                hideLoadingBar()
                Toast.makeText(context, "تعذر قراءة الملف: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveToHistory(uri: Uri) {
        try {
            val cursor = requireContext().contentResolver.query(
                uri, arrayOf(android.provider.MediaStore.MediaColumns.DATA), null, null, null
            )
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                    if (idx >= 0 && !c.isNull(idx)) {
                        val path = c.getString(idx)
                        if (path != null) HistoryFragment.addToHistory(requireContext(), path)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun loadSTLFile(uri: Uri) = loadFile(uri)

    private fun showLoadingBar(msg: String, progress: Int) {
        if (view == null || !isAdded) return
        loadingContainer.visibility = View.VISIBLE
        loadingContainer.alpha = 1f
        loadingProgress.progress = progress
        loadingText.text = msg
    }

    private fun updateLoadingBar(msg: String, progress: Int) {
        if (view == null || !isAdded) return
        loadingProgress.progress = progress
        loadingText.text = msg
    }

    private fun hideLoadingBar() {
        if (view == null || !isAdded) return
        loadingContainer.animate().alpha(0f).setDuration(300).withEndAction {
            if (isAdded && view != null) {
                loadingContainer.visibility = View.GONE
                loadingContainer.alpha = 1f
            }
        }.start()
    }

    private fun jumpToView(targetRotX: Float, targetRotY: Float) {
        val r = glViewerView.stlRenderer
        val sx = r.rotationX; val sy = r.rotationY
        android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300; interpolator = DecelerateInterpolator(2f)
            addUpdateListener { a ->
                val t = a.animatedValue as Float
                r.rotationX = sx + (targetRotX - sx) * t
                r.rotationY = sy + (targetRotY - sy) * t
            }
        }.start()
    }

    private fun resetCamera() {
        val r = glViewerView.stlRenderer
        r.rotationX = -25f; r.rotationY = 35f
        r.scaleFactor = 1f; r.panX = 0f; r.panY = 0f
        glViewerView.queueEvent { r.updateProjection() }
    }

    // ══ قائمة الخامات كـ Grid ══
    private fun showMaterialGrid() {
        val materials = STLRenderer.Material.values()
        val matNames  = materials.map { it.nameAr }.toTypedArray()
        val bgNames   = arrayOf("🌑 داكن","⬛ أسود","🌫️ رمادي","⬜ أبيض","🌊 كحلي")
        val bgColors  = listOf(
            floatArrayOf(0.10f,0.11f,0.13f), floatArrayOf(0.02f,0.02f,0.02f),
            floatArrayOf(0.22f,0.24f,0.27f), floatArrayOf(0.92f,0.92f,0.92f),
            floatArrayOf(0.05f,0.08f,0.18f)
        )

        val ctx = requireContext()
        val dialog = AlertDialog.Builder(ctx).create()

        // Layout شبكي
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 16)
        }

        // عنوان
        root.addView(TextView(ctx).apply {
            text = "🎨  اختر الخامة"
            textSize = 16f; setTextColor(0xFFFF8A1E.toInt())
            setPadding(0, 0, 0, 16)
        })

        // شبكة الخامات
        val matGrid = android.widget.GridLayout(ctx).apply {
            columnCount = 4; rowCount = 2
        }
        materials.forEachIndexed { i, mat ->
            val btn = Button(ctx).apply {
                text = mat.nameAr; textSize = 9f
                background = ctx.getDrawable(R.drawable.bg_material_ball)
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = android.widget.GridLayout.LayoutParams().apply {
                    width = 0; height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = android.widget.GridLayout.spec(i % 4, 1f)
                    rowSpec    = android.widget.GridLayout.spec(i / 4)
                    setMargins(4, 4, 4, 4)
                }
                setOnClickListener {
                    glViewerView.stlRenderer.setMaterial(mat)
                    Toast.makeText(ctx, mat.nameAr, Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            matGrid.addView(btn)
        }
        root.addView(matGrid)

        // فاصل
        root.addView(TextView(ctx).apply {
            text = "── خلفية ──"; textSize = 12f
            setTextColor(0xFF888888.toInt()); setPadding(0, 16, 0, 8)
        })

        // شبكة الخلفيات
        val bgRow = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        bgNames.forEachIndexed { i, name ->
            bgRow.addView(Button(ctx).apply {
                text = name; textSize = 9f
                setTextColor(0xFFCCCCCC.toInt())
                background = ctx.getDrawable(R.drawable.bg_toggle_button)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    val c = bgColors[i]
                    glViewerView.stlRenderer.setBackgroundColor(c[0], c[1], c[2])
                    dialog.dismiss()
                }
            })
        }
        root.addView(bgRow)

        dialog.setView(root)
        dialog.show()
    }

    private fun cycleUnit() {
        currentUnit = when (currentUnit) {
            MeasurementUnit.MM   -> MeasurementUnit.CM
            MeasurementUnit.CM   -> MeasurementUnit.INCH
            MeasurementUnit.INCH -> MeasurementUnit.MM
        }
        btnUnit.text = "📏 ${currentUnit.label}"
        currentModel?.let { if (inspectionCard.visibility == View.VISIBLE) showInspectionReport(it) }
        val pts = glViewerView.stlRenderer.getMeasurementPoints()
        if (pts.size == 2) updateMeasurementText(pts[0], pts[1])
    }

    private fun exportCurrentView() {
        if (currentModel == null) { Toast.makeText(context,"افتح ملف أولاً",Toast.LENGTH_SHORT).show(); return }
        val r = glViewerView.stlRenderer
        val w = r.getSurfaceWidth(); val h = r.getSurfaceHeight()
        if (w <= 0 || h <= 0) return
        glViewerView.queueEvent {
            val bmp = r.captureFrame(w, h)
            requireActivity().runOnUiThread { saveAndShareBitmap(bmp) }
        }
    }

    private fun saveAndShareBitmap(bitmap: Bitmap) {
        try {
            val file = File(requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "Amr3D_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(requireContext(),
                "${requireContext().packageName}.fileprovider", file)
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "تصدير الصورة"))
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر الحفظ", Toast.LENGTH_LONG).show()
        }
    }

    private fun openWhatsapp() {
        val phone = "201009172167"
        val msg = Uri.encode("مرحبًا، عندي استفسار بخصوص تطبيق Amr3D Preview")
        try {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("whatsapp://send?phone=$phone&text=$msg")).apply { setPackage("com.whatsapp") })
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=$msg")))
        }
    }

    private fun handleMeasurementTap(screenX: Float, screenY: Float) {
        val model = currentModel ?: return
        val r = glViewerView.stlRenderer
        val ray = RayPicker.screenPointToRay(screenX, screenY,
            r.getSurfaceWidth(), r.getSurfaceHeight(),
            r.getCurrentModelMatrix(), r.getCurrentViewMatrix(), r.getCurrentProjectionMatrix())
        val hit = RayPicker.findClosestIntersection(ray, model) ?: run {
            Toast.makeText(context, "لم يتم تحديد نقطة", Toast.LENGTH_SHORT).show(); return
        }
        r.addMeasurementPoint(hit)
        val pts = r.getMeasurementPoints()
        if (pts.size == 2) updateMeasurementText(pts[0], pts[1])
        else {
            measurementText.text = "نقطة أولى محددة — اضغط على نقطة ثانية"
            measurementCard.visibility = View.VISIBLE
        }
    }

    private fun updateMeasurementText(p1: FloatArray, p2: FloatArray) {
        val d = MeasurementTools.distanceBetween(p1, p2, currentUnit)
        measurementText.text = String.format(Locale.US, "المسافة: %.3f %s", d, currentUnit.label)
        measurementCard.visibility = View.VISIBLE
    }

    private fun showInspectionReport(model: STLModel) {
        if (inspectionCard.visibility == View.VISIBLE) { inspectionCard.visibility = View.GONE; return }
        val report = MeasurementTools.inspect(model, currentUnit)
        val u = report.unit.label
        inspectionText.text = "📐 أبعاد الموديل\n─────────────────\n" +
            "الطول (X):    ${"%.2f".format(report.width)} $u\n" +
            "العرض (Y):   ${"%.2f".format(report.depth)} $u\n" +
            "الارتفاع (Z): ${"%.2f".format(report.height)} $u"
        inspectionCard.visibility  = View.VISIBLE
        measurementCard.visibility = View.GONE
    }
}
