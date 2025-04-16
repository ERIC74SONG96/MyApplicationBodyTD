package com.example.myapplicationbodytd

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.myapplicationbodytd.enemies.Enemy
import com.example.myapplicationbodytd.managers.GameManager
import com.example.myapplicationbodytd.player.Player
import com.example.myapplicationbodytd.towers.Tower
import com.example.myapplicationbodytd.towers.TowerType
import com.example.myapplicationbodytd.ui.Map
import kotlin.math.sin
import kotlin.math.cos

/**
 * GameView est la vue principale du jeu qui gère le rendu et les interactions utilisateur.
 * Elle hérite de SurfaceView pour permettre le rendu personnalisé.
 * 
 * Responsabilités :
 * - Gestion du cycle de vie de la surface de rendu
 * - Boucle de jeu principale
 * - Rendu des différents états du jeu (menu, jeu, game over)
 * - Gestion des événements tactiles
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Runnable {

    private var gameThread: Thread? = null
    private var isRunning = false
    private val gameManager = GameManager.getInstance(context)
    private val paint = Paint()
    private var lastFrameTime = System.nanoTime()
    private val targetFrameTime = 16_666_666L // ~60 FPS
    private val surfaceHolder: SurfaceHolder = holder
    private var isGameStarted = false
    private val startButtonBounds = RectF()
    private val replayButtonBounds = RectF()
    private var animationTime = 0f
    private val backgroundGradient = LinearGradient(0f, 0f, 0f, 0f, 
        intArrayOf(
            Color.parseColor("#1a2a6c"),
            Color.parseColor("#b21f1f"),
            Color.parseColor("#fdbb2d")
        ), null, Shader.TileMode.CLAMP)
    private val menuGradient = LinearGradient(0f, 0f, 0f, 0f,
        intArrayOf(
            Color.parseColor("#0f2027"),
            Color.parseColor("#203a43"),
            Color.parseColor("#2c5364")
        ), null, Shader.TileMode.CLAMP)
    private val buttonGradient = LinearGradient(0f, 0f, 0f, 0f,
        intArrayOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#45a049")
        ), null, Shader.TileMode.CLAMP)

    init {
        holder.addCallback(this)
        isFocusable = true
        setZOrderMediaOverlay(true) // Permet aux éléments d'interface d'être visibles
        holder.setFormat(PixelFormat.TRANSLUCENT)
        Log.d("GameView", "GameView initialized")
        setupGameManagerListeners()
    }

    /**
     * Initialise la vue et configure les listeners du GameManager.
     */
    private fun setupGameManagerListeners() {
        gameManager.setOnGameOverListener { 
            // Gérer le game over
        }
        gameManager.setOnWaveCompleteListener { wave ->
            // Gérer la fin de vague
        }
        gameManager.setOnMoneyChangedListener { money ->
            // Mettre à jour l'interface avec l'argent
        }
        gameManager.setOnHealthChangedListener { health ->
            // Mettre à jour l'interface avec la santé
        }
        gameManager.setOnScoreChangedListener { score ->
            // Mettre à jour l'interface avec le score
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            gameManager.setScreenDimensions(w, h)
            val buttonWidth = w * 0.6f
            val buttonHeight = h * 0.1f
            val buttonX = (w - buttonWidth) / 2
            val buttonY = h * 0.4f
            startButtonBounds.set(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
            replayButtonBounds.set(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
            
            // Mettre à jour les dégradés avec les nouvelles dimensions
            backgroundGradient.setLocalMatrix(Matrix().apply { setScale(1f, h.toFloat()) })
            menuGradient.setLocalMatrix(Matrix().apply { setScale(1f, h.toFloat()) })
            buttonGradient.setLocalMatrix(Matrix().apply { setScale(1f, buttonHeight) })
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        lastFrameTime = System.nanoTime()
        gameThread = Thread(this).apply {
            name = "GameThread"
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        gameManager.setScreenDimensions(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        gameThread?.join()
    }

    override fun run() {
        while (isRunning) {
            val currentTime = System.nanoTime()
            val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
            lastFrameTime = currentTime

            update(deltaTime)
            draw()
        }
    }

    /**
     * Met à jour l'état du jeu.
     * Appelle la méthode update du GameManager si le jeu est en cours.
     * 
     * @param deltaTime Temps écoulé depuis la dernière frame en secondes
     */
    private fun update(deltaTime: Float) {
        if (isGameStarted && !gameManager.isGameOver()) {
            try {
                synchronized(gameManager) {
                    gameManager.update(deltaTime)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Dessine l'état actuel du jeu.
     * Gère le rendu en fonction de l'état du jeu (menu, jeu, game over).
     */
    private fun draw() {
        var canvas: Canvas? = null
        try {
            canvas = surfaceHolder.lockCanvas()
            canvas?.let {
                it.drawColor(Color.BLACK)

                when {
                    !isGameStarted -> drawStartMenu(it)
                    gameManager.isGameOver() -> drawGameOver(it)
                    else -> drawGame(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            canvas?.let { surfaceHolder.unlockCanvasAndPost(it) }
        }
    }

    private fun drawGame(canvas: Canvas) {
        // Dessiner le fond animé
        drawAnimatedBackground(canvas)
        
        canvas.save()
        canvas.clipRect(0f, 0f, width.toFloat(), gameManager.getGameAreaBottom())
        
        // Réduire l'effet de flou pour l'arrière-plan
        paint.maskFilter = BlurMaskFilter(5f, BlurMaskFilter.Blur.OUTER)
        gameManager.draw(canvas, paint)
        paint.maskFilter = null
        
        // Dessiner les contours des éléments du jeu
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.WHITE
        paint.alpha = 50
        gameManager.draw(canvas, paint)
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        
        canvas.restore()

        // Dessiner les informations du jeu avec un style amélioré
        drawGameInfo(canvas)

        // Menu des tours avec effets visuels
        if (isGameStarted && !gameManager.isGameOver()) {
            drawTowerMenu(canvas)
        }
    }

    private fun drawAnimatedBackground(canvas: Canvas) {
        animationTime += 0.01f
        
        // Fond dégradé animé
        val gradient = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                Color.parseColor("#1a2a6c"),
                Color.parseColor("#b21f1f"),
                Color.parseColor("#fdbb2d")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        paint.alpha = 200
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.alpha = 255
        paint.shader = null
        
        // Particules animées
        paint.color = Color.argb(30, 255, 255, 255)
        for (i in 0..20) {
            val x = width * (0.1f + 0.8f * sin(animationTime + i * 0.3f))
            val y = height * (0.1f + 0.8f * cos(animationTime + i * 0.2f))
            val radius = 30f + 15f * sin(animationTime * 2 + i * 0.1f)
            canvas.drawCircle(x, y, radius, paint)
        }
        
        // Effet de grille
        paint.color = Color.argb(20, 255, 255, 255)
        paint.strokeWidth = 1f
        val gridSize = 50f
        for (x in 0..width step gridSize.toInt()) {
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
        }
        for (y in 0..height step gridSize.toInt()) {
            canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
        }
        
        // Effet de pulsation
        paint.color = Color.argb(15, 255, 255, 255)
        val pulseRadius = 100f + 20f * sin(animationTime * 3)
        canvas.drawCircle(width / 2f, height / 2f, pulseRadius, paint)
        
        // Effet de particules rapides
        paint.color = Color.argb(40, 255, 255, 255)
        for (i in 0..10) {
            val speed = 0.5f
            val x = width * (0.1f + 0.8f * sin(animationTime * speed + i * 0.5f))
            val y = height * (0.1f + 0.8f * cos(animationTime * speed + i * 0.3f))
            val size = 5f + 3f * sin(animationTime * 2 + i * 0.2f)
            canvas.drawCircle(x, y, size, paint)
        }
    }

    private fun drawGameInfo(canvas: Canvas) {
        val padding = 20f
        val textSize = 40f
        val spacing = 40f
        var startX = padding
        val topPadding = 20f
        val backgroundHeight = textSize + padding * 2

        // Fond avec effet de verre amélioré
        paint.color = Color.argb(180, 0, 0, 0)
        paint.maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), backgroundHeight, 20f, 20f, paint)
        paint.maskFilter = null

        // Effet de bordure lumineuse
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.argb(100, 255, 255, 255)
        canvas.drawRoundRect(0f, 0f, width.toFloat(), backgroundHeight, 20f, 20f, paint)
        paint.style = Paint.Style.FILL

        paint.textSize = textSize
        paint.textAlign = Paint.Align.LEFT

        // Santé avec effet de pulsation amélioré
        val health = gameManager.getHealth()
        val healthColor = when {
            health < 30 -> Color.RED
            health < 50 -> Color.YELLOW
            else -> Color.WHITE
        }
        paint.color = healthColor
        val pulseScale = 1f + 0.15f * sin(animationTime * 2)
        canvas.save()
        canvas.scale(pulseScale, pulseScale, startX, textSize + topPadding)
        paint.setShadowLayer(8f, 0f, 0f, healthColor)
        canvas.drawText("❤ $health", startX, textSize + topPadding, paint)
        paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)
        canvas.restore()
        startX += paint.measureText("❤ $health") + spacing

        // Argent avec effet de brillance amélioré
        val money = gameManager.getMoney()
        paint.color = when {
            money < 50 -> Color.RED
            else -> Color.WHITE
        }
        paint.setShadowLayer(10f, 0f, 0f, Color.parseColor("#FFD700"))
        canvas.drawText("💰 $money", startX, textSize + topPadding, paint)
        paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)
        startX += paint.measureText("💰 $money") + spacing

        // Vague avec animation améliorée
        paint.color = Color.WHITE
        paint.setShadowLayer(8f, 0f, 0f, Color.BLUE)
        canvas.drawText("🌊 ${gameManager.getCurrentWave() + 1}", startX, textSize + topPadding, paint)
        paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)
        startX += paint.measureText("🌊 ${gameManager.getCurrentWave() + 1}") + spacing

        // Score avec effet de brillance amélioré
        paint.color = Color.WHITE
        paint.setShadowLayer(10f, 0f, 0f, Color.parseColor("#FFD700"))
        canvas.drawText("🏆 ${gameManager.getScore()}", startX, textSize + topPadding, paint)
        paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)
    }

    private fun drawStartMenu(canvas: Canvas) {
        // Fond animé
        paint.shader = menuGradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Titre avec effet de brillance
        paint.color = Color.WHITE
        paint.textSize = 100f
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        paint.setShadowLayer(15f, 0f, 0f, Color.BLUE)
        canvas.drawText("Body TD", width / 2f, height * 0.25f, paint)
        paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)

        // Bouton avec effet de survol
        paint.shader = buttonGradient
        paint.setShadowLayer(12f, 0f, 8f, Color.BLACK)
        canvas.drawRoundRect(startButtonBounds, 40f, 40f, paint)
        paint.shader = null

        // Texte du bouton
        paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)
        paint.color = Color.WHITE
        paint.textSize = 50f
        canvas.drawText("Commencer", startButtonBounds.centerX(), startButtonBounds.centerY() + paint.textSize / 3, paint)
    }

    private fun drawTowerMenu(canvas: Canvas) {
        val buttonHeight = gameManager.getTowerMenuHeight() / 3
        val buttonY = gameManager.getGameAreaBottom()

        val towerTypes = listOf(TowerType.BASIC, TowerType.SNIPER, TowerType.RAPID)
        val towerNames = listOf("Basique", "Sniper", "Rapide")

        // Fond du menu avec effet de verre
        paint.color = Color.argb(180, 0, 0, 0)
        paint.maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRoundRect(0f, buttonY, width.toFloat(), height.toFloat(), 30f, 30f, paint)
        paint.maskFilter = null

        // Effet de bordure lumineuse
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.argb(100, 255, 255, 255)
        canvas.drawRoundRect(0f, buttonY, width.toFloat(), height.toFloat(), 30f, 30f, paint)
        paint.style = Paint.Style.FILL

        for (i in towerTypes.indices) {
            val top = buttonY + buttonHeight * i
            val bottom = top + buttonHeight

            // Fond du bouton avec dégradé amélioré
            val buttonGradient = LinearGradient(
                0f, top, 0f, bottom,
                towerTypes[i].color,
                Color.argb(200, Color.red(towerTypes[i].color), 
                          Color.green(towerTypes[i].color), 
                          Color.blue(towerTypes[i].color)),
                Shader.TileMode.CLAMP
            )
            paint.shader = buttonGradient
            paint.setShadowLayer(8f, 0f, 4f, Color.DKGRAY)
            canvas.drawRoundRect(RectF(0f, top, width.toFloat(), bottom), 25f, 25f, paint)
            paint.shader = null

            // Effet de survol
            if (gameManager.getSelectedTowerType() == towerTypes[i]) {
                paint.color = Color.argb(50, 255, 255, 255)
                canvas.drawRoundRect(RectF(0f, top, width.toFloat(), bottom), 25f, 25f, paint)
            }

            // Texte avec effet de brillance amélioré
            paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)
            paint.color = Color.WHITE
            paint.textSize = 36f
            paint.textAlign = Paint.Align.CENTER
            paint.setShadowLayer(5f, 0f, 0f, Color.WHITE)
            canvas.drawText("${towerNames[i]} - ${towerTypes[i].cost}$", 
                          width / 2f, top + buttonHeight / 2 + 10f, paint)
            paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)
        }
    }

    private fun drawGameOver(canvas: Canvas) {
        // Fond semi-transparent avec effet de flou
        paint.color = Color.argb(200, 0, 0, 0)
        paint.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.maskFilter = null

        // Texte Game Over avec effet amélioré
        paint.color = Color.WHITE
        paint.textSize = 80f
        paint.textAlign = Paint.Align.CENTER
        paint.setShadowLayer(15f, 0f, 0f, Color.RED)
        canvas.drawText("Game Over", width / 2f, height * 0.3f, paint)
        paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)

        // Score avec effet de brillance amélioré
        paint.textSize = 40f
        paint.setShadowLayer(10f, 0f, 0f, Color.parseColor("#FFD700"))
        canvas.drawText("Score: ${gameManager.getScore()}", width / 2f, height * 0.4f, paint)
        paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)

        // Bouton Rejouer avec effet de survol amélioré
        paint.shader = buttonGradient
        paint.setShadowLayer(12f, 0f, 6f, Color.BLACK)
        canvas.drawRoundRect(replayButtonBounds, 30f, 30f, paint)
        paint.shader = null

        // Effet de survol sur le bouton
        paint.color = Color.argb(50, 255, 255, 255)
        canvas.drawRoundRect(replayButtonBounds, 30f, 30f, paint)

        // Texte du bouton avec effet amélioré
        paint.color = Color.WHITE
        paint.textSize = 40f
        paint.setShadowLayer(5f, 0f, 0f, Color.WHITE)
        canvas.drawText("Rejouer", replayButtonBounds.centerX(), 
                       replayButtonBounds.centerY() + paint.textSize / 3, paint)
        paint.setShadowLayer(0f, 0f, 0f, Color.BLACK)
    }

    /**
     * Gère les événements tactiles.
     * 
     * @param event L'événement tactile
     * @return true si l'événement a été traité, false sinon
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y

                if (!isGameStarted) {
                    if (startButtonBounds.contains(x, y)) {
                        startGame()
                        return true
                    }
                    return false
                } else if (gameManager.isGameOver()) {
                    if (replayButtonBounds.contains(x, y)) {
                        restartGame()
                        return true
                    }
                    return false
                }

                if (y >= gameManager.getGameAreaBottom()) {
                    handleTowerMenuTap(x, y)
                    return true
                }

                if (y < gameManager.getGameAreaBottom()) {
                    gameManager.handleGameAreaTap(x, y)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Démarre une nouvelle partie.
     * Initialise l'état du jeu et démarre la boucle de jeu.
     */
    private fun startGame() {
        isGameStarted = true
        gameManager.startGame()
    }

    private fun restartGame() {
        isGameStarted = true
        gameManager.startGame()
    }

    private fun handleTowerMenuTap(x: Float, y: Float) {
        val buttonHeight = gameManager.getTowerMenuHeight() / 3
        val buttonY = gameManager.getGameAreaBottom()

        when {
            y < buttonY + buttonHeight -> gameManager.selectTowerType(TowerType.BASIC)
            y < buttonY + buttonHeight * 2 -> gameManager.selectTowerType(TowerType.SNIPER)
            else -> gameManager.selectTowerType(TowerType.RAPID)
        }
    }

    /**
     * Reprend le jeu après une pause.
     * Relance la boucle de jeu.
     */
    fun resume() {
        if (!isRunning) {
            isRunning = true
            lastFrameTime = System.nanoTime()
            gameThread = Thread(this).apply { start() }
        }
    }

    /**
     * Met le jeu en pause.
     * Arrête la boucle de jeu.
     */
    fun pause() {
        isRunning = false
        gameThread?.join()
    }

    fun cleanup() {
        isRunning = false
        try {
            gameThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        holder.surface.release()
    }
}
