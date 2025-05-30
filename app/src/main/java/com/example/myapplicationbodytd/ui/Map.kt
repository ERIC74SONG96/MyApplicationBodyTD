package com.example.myapplicationbodytd.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.example.myapplicationbodytd.managers.GameManager
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Map(private val gameManager: GameManager) {
    private val path = Path()
    private val wayPoints = mutableListOf<PointF>()
    private val towerPlacements = mutableSetOf<PointF>()
    
    private val pathWidth = 30f
    private val minTowerDistance = 80f
    private val gridSize = 50f
    private var animationProgress = 0f
    private val pathGlowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = pathWidth + 20f
        color = Color.argb(50, 255, 255, 255)
        maskFilter = android.graphics.BlurMaskFilter(15f, android.graphics.BlurMaskFilter.Blur.OUTER)
    }

    private val bodyPaint = Paint().apply {
        color = Color.argb(50, 200, 200, 200)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    
    private val organPaint = Paint().apply {
        color = Color.argb(30, 150, 150, 150)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        initializePath()
    }

    private fun initializePath() {
        val screenWidth = gameManager.getScreenWidth()
        val screenHeight = gameManager.getScreenHeight()
        
        // Points de contrôle pour un chemin qui ressemble à un intestin
        wayPoints.clear()
        wayPoints.add(PointF(0f, screenHeight / 2f))
        
        // Premier segment - entrée de l'intestin
        wayPoints.add(PointF(screenWidth * 0.1f, screenHeight * 0.4f))
        wayPoints.add(PointF(screenWidth * 0.2f, screenHeight * 0.6f))
        
        // Deuxième segment - première boucle
        wayPoints.add(PointF(screenWidth * 0.3f, screenHeight * 0.3f))
        wayPoints.add(PointF(screenWidth * 0.4f, screenHeight * 0.7f))
        wayPoints.add(PointF(screenWidth * 0.5f, screenHeight * 0.4f))
        
        // Troisième segment - deuxième boucle
        wayPoints.add(PointF(screenWidth * 0.6f, screenHeight * 0.6f))
        wayPoints.add(PointF(screenWidth * 0.7f, screenHeight * 0.3f))
        wayPoints.add(PointF(screenWidth * 0.8f, screenHeight * 0.5f))
        
        // Dernier segment - sortie de l'intestin
        wayPoints.add(PointF(screenWidth * 0.9f, screenHeight * 0.4f))
        wayPoints.add(PointF(screenWidth.toFloat(), screenHeight / 2f))

        // Création du chemin avec des courbes de Bézier plus organiques
        path.reset()
        path.moveTo(wayPoints[0].x, wayPoints[0].y)
        
        for (i in 0 until wayPoints.size - 1) {
            val current = wayPoints[i]
            val next = wayPoints[i + 1]
            
            // Calcul des points de contrôle pour des courbes plus organiques
            val controlX1 = current.x + (next.x - current.x) * 0.3f
            val controlY1 = current.y + (next.y - current.y) * 0.3f
            val controlX2 = current.x + (next.x - current.x) * 0.7f
            val controlY2 = current.y + (next.y - current.y) * 0.7f
            
            path.cubicTo(controlX1, controlY1, controlX2, controlY2, next.x, next.y)
        }

        // Ajout d'emplacements de tours stratégiques le long du chemin
        towerPlacements.clear()
        
        // Zone 1: Défense initiale
        towerPlacements.add(PointF(screenWidth * 0.1f, screenHeight * 0.35f))
        towerPlacements.add(PointF(screenWidth * 0.1f, screenHeight * 0.65f))
        
        // Zone 2: Première boucle
        towerPlacements.add(PointF(screenWidth * 0.3f, screenHeight * 0.25f))
        towerPlacements.add(PointF(screenWidth * 0.3f, screenHeight * 0.75f))
        
        // Zone 3: Deuxième boucle
        towerPlacements.add(PointF(screenWidth * 0.5f, screenHeight * 0.35f))
        towerPlacements.add(PointF(screenWidth * 0.5f, screenHeight * 0.65f))
        
        // Zone 4: Troisième boucle
        towerPlacements.add(PointF(screenWidth * 0.7f, screenHeight * 0.25f))
        towerPlacements.add(PointF(screenWidth * 0.7f, screenHeight * 0.75f))
        
        // Zone 5: Défense finale
        towerPlacements.add(PointF(screenWidth * 0.9f, screenHeight * 0.35f))
        towerPlacements.add(PointF(screenWidth * 0.9f, screenHeight * 0.65f))
    }

    fun updatePath() {
        initializePath()
    }

    fun draw(canvas: Canvas, paint: Paint) {
        val screenWidth = canvas.width
        val screenHeight = canvas.height

        // Mise à jour de l'animation
        animationProgress = (animationProgress + 0.01f) % 1f

        // Dessiner la grille avec effet de profondeur
        drawGrid(canvas, paint, screenWidth, screenHeight)

        // Dessiner le chemin avec effet de brillance
        drawPath(canvas, paint)

        // Dessiner le fond (corps humain)
        drawBody(canvas)
        
        // Dessiner les organes
        drawOrgans(canvas)
    }

    private fun drawGrid(canvas: Canvas, paint: Paint, screenWidth: Int, screenHeight: Int) {
        // Fond organique avec des cellules
        paint.color = Color.argb(20, 200, 200, 200)
        paint.style = Paint.Style.FILL
        
        // Dessiner des cellules organiques
        val cellSize = gridSize * 2
        for (x in 0..screenWidth step cellSize.toInt()) {
            for (y in 0..screenHeight step cellSize.toInt()) {
                // Effet de pulsation pour chaque cellule
                val pulse = 1f + 0.1f * sin(animationProgress * 2 * PI.toFloat() + x * 0.01f + y * 0.01f)
                
                // Dessiner une cellule organique
                paint.color = Color.argb(20, 200, 200, 200)
                canvas.drawCircle(x.toFloat(), y.toFloat(), cellSize/2 * pulse, paint)
                
                // Ajouter des détails à la cellule
                paint.color = Color.argb(10, 255, 255, 255)
                canvas.drawCircle(x.toFloat(), y.toFloat(), cellSize/3 * pulse, paint)
            }
        }

        // Ajouter des vaisseaux sanguins subtils
        paint.color = Color.argb(15, 150, 0, 0)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        
        // Dessiner des vaisseaux sanguins horizontaux
        for (y in 0..screenHeight step (gridSize * 3).toInt()) {
            val path = Path()
            path.moveTo(0f, y.toFloat())
            for (x in 0..screenWidth step gridSize.toInt()) {
                val offset = sin(animationProgress * 2 * PI.toFloat() + x * 0.01f) * 10f
                path.lineTo(x.toFloat(), y.toFloat() + offset)
            }
            canvas.drawPath(path, paint)
        }
        
        // Dessiner des vaisseaux sanguins verticaux
        for (x in 0..screenWidth step (gridSize * 3).toInt()) {
            val path = Path()
            path.moveTo(x.toFloat(), 0f)
            for (y in 0..screenHeight step gridSize.toInt()) {
                val offset = cos(animationProgress * 2 * PI.toFloat() + y * 0.01f) * 10f
                path.lineTo(x.toFloat() + offset, y.toFloat())
            }
            canvas.drawPath(path, paint)
        }
    }

    private fun drawPath(canvas: Canvas, paint: Paint) {
        // Effet de brillance du chemin
        canvas.drawPath(path, pathGlowPaint)

        // Chemin principal
        paint.color = Color.argb(200, 100, 100, 100)
        paint.strokeWidth = pathWidth
        paint.style = Paint.Style.STROKE
        
        // Effet de texture sur le chemin
        val pathMeasure = PathMeasure(path, false)
        val length = pathMeasure.length
        val numPoints = 100
        
        for (i in 0 until numPoints) {
            val pos = FloatArray(2)
            val tan = FloatArray(2)
            val distance = length * i / numPoints
            pathMeasure.getPosTan(distance, pos, tan)
            
            // Effet de brillance qui se déplace le long du chemin
            val alpha = (sin(animationProgress * 2 * PI.toFloat() + i * 0.1f) * 50 + 50).toInt()
            paint.color = Color.argb(alpha, 150, 150, 150)
            canvas.drawCircle(pos[0], pos[1], pathWidth/2, paint)
        }
        
        // Dessiner le chemin principal
        paint.color = Color.argb(200, 100, 100, 100)
        canvas.drawPath(path, paint)
    }

    private fun drawBody(canvas: Canvas) {
        val screenWidth = gameManager.getScreenWidth().toFloat()
        val screenHeight = gameManager.getScreenHeight().toFloat()
        
        // Dessiner le contour du corps
        val bodyPath = Path()
        bodyPath.moveTo(screenWidth * 0.3f, 0f)  // Tête
        bodyPath.lineTo(screenWidth * 0.7f, 0f)
        bodyPath.lineTo(screenWidth * 0.8f, screenHeight * 0.2f)  // Épaules
        bodyPath.lineTo(screenWidth * 0.9f, screenHeight * 0.3f)
        bodyPath.lineTo(screenWidth, screenHeight * 0.4f)
        bodyPath.lineTo(screenWidth, screenHeight * 0.8f)  // Hanches
        bodyPath.lineTo(screenWidth * 0.8f, screenHeight)
        bodyPath.lineTo(screenWidth * 0.2f, screenHeight)
        bodyPath.lineTo(0f, screenHeight * 0.8f)
        bodyPath.lineTo(0f, screenHeight * 0.4f)
        bodyPath.lineTo(screenWidth * 0.1f, screenHeight * 0.3f)
        bodyPath.lineTo(screenWidth * 0.2f, screenHeight * 0.2f)
        bodyPath.close()
        
        canvas.drawPath(bodyPath, bodyPaint)
    }

    private fun drawOrgans(canvas: Canvas) {
        val screenWidth = gameManager.getScreenWidth().toFloat()
        val screenHeight = gameManager.getScreenHeight().toFloat()
        
        // Cœur
        val heartPath = Path()
        heartPath.moveTo(screenWidth * 0.4f, screenHeight * 0.2f)
        heartPath.quadTo(screenWidth * 0.5f, screenHeight * 0.1f, screenWidth * 0.6f, screenHeight * 0.2f)
        heartPath.quadTo(screenWidth * 0.7f, screenHeight * 0.3f, screenWidth * 0.5f, screenHeight * 0.4f)
        heartPath.quadTo(screenWidth * 0.3f, screenHeight * 0.3f, screenWidth * 0.4f, screenHeight * 0.2f)
        canvas.drawPath(heartPath, organPaint)
        
        // Poumons
        canvas.drawOval(RectF(screenWidth * 0.3f, screenHeight * 0.25f, screenWidth * 0.45f, screenHeight * 0.35f), organPaint)
        canvas.drawOval(RectF(screenWidth * 0.55f, screenHeight * 0.25f, screenWidth * 0.7f, screenHeight * 0.35f), organPaint)
        
        // Estomac
        canvas.drawOval(RectF(screenWidth * 0.25f, screenHeight * 0.35f, screenWidth * 0.35f, screenHeight * 0.45f), organPaint)
        
        // Foie
        canvas.drawOval(RectF(screenWidth * 0.35f, screenHeight * 0.4f, screenWidth * 0.45f, screenHeight * 0.5f), organPaint)
        
        // Intestins
        val intestinePath = Path()
        intestinePath.moveTo(screenWidth * 0.35f, screenHeight * 0.45f)
        for (i in 1..10) {
            val x = screenWidth * (0.35f + i * 0.05f)
            val y = screenHeight * (0.45f + 0.05f * sin(i * PI.toFloat() / 2))
            intestinePath.lineTo(x, y)
        }
        canvas.drawPath(intestinePath, organPaint)
    }

    fun isValidTowerLocation(x: Float, y: Float): Boolean {
        val point = PointF(x, y)
        val screenWidth = gameManager.getScreenWidth()
        val screenHeight = gameManager.getScreenHeight()
        
        // Vérifier si le point est trop proche du chemin
        val nearPath = isPointNearPath(point)
        if (nearPath) {
            Log.d("Map", "Point trop proche du chemin: ($x,$y)")
            return false
        }

        // Vérifier si le point est trop proche d'une autre tour
        val nearTower = isPointNearTower(point)
        if (nearTower) {
            Log.d("Map", "Point trop proche d'une autre tour: ($x,$y)")
            return false
        }

        //Vérifier si le point est dans les limites de l'écran
        val inBounds = x >= minTowerDistance && x <= screenWidth - minTowerDistance &&
                      y >= minTowerDistance && y <= screenHeight - minTowerDistance
        if (!inBounds) {
            Log.d("Map", "Point hors limites: ($x,$y)")
            return false
        }

        Log.d("Map", "Emplacement valide pour une tour: ($x,$y)")
        return true
    }

    private fun isPointNearPath(point: PointF): Boolean {
        val pathMeasure = PathMeasure(path, false)
        var minDistance = Float.MAX_VALUE
        
        for (i in 0..100) {
            val pathPoint = FloatArray(2)
            pathMeasure.getPosTan(pathMeasure.length * i / 100f, pathPoint, null)
            val dist = calculateDistance(point.x, point.y, pathPoint[0], pathPoint[1])
            minDistance = minOf(minDistance, dist)
        }
        
        val isNear = minDistance < pathWidth
        if (isNear) {
            Log.d("Map", "Distance minimale au chemin: $minDistance (limite: $pathWidth)")
        }
        return isNear
    }

    private fun isPointNearTower(point: PointF): Boolean {
        return towerPlacements.any { towerPoint ->
            calculateDistance(point.x, point.y, towerPoint.x, towerPoint.y) < minTowerDistance
        }
    }

    private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return kotlin.math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))
    }

    fun addTowerPlacement(point: PointF) {
        towerPlacements.add(point)
    }

    fun getWayPoints(): List<PointF> = wayPoints.toList()

}
