package com.example.arbees

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.arbees.databinding.ActivityMainBinding
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.math.Position
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.Node
import java.util.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var score = 0
    private var timer: CountDownTimer? = null
    private val GAME_DURATION = 60000L // 60 seconds
    private val SPAWN_DISTANCE = 3.0f // 3 meters
    private val random = Random()

    private val bees = mutableListOf<Node>()
    private val animators = mutableListOf<ValueAnimator>()

    private lateinit var soundPool: SoundPool
    private var zapSoundId: Int = 0
    private var isSoundLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initSoundPool()

        binding.btnShoot.setOnClickListener {
            shoot()
        }

        binding.btnRestart.setOnClickListener {
            startGame()
        }

        startGame()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) {
                isSoundLoaded = true
            }
        }

        zapSoundId = soundPool.load(this, R.raw.laser_zap, 1)
    }

    private fun startGame() {
        score = 0
        updateScore()
        binding.gameOverLayout.visibility = View.GONE
        binding.btnShoot.isEnabled = true

        clearBees()
        spawnWave()

        timer?.cancel()
        timer = object : CountDownTimer(GAME_DURATION, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.tvTimer.text = "Time: ${millisUntilFinished / 1000}"
            }

            override fun onFinish() {
                endGame()
            }
        }.start()
    }

    private fun endGame() {
        binding.tvTimer.text = "Time: 0"
        binding.btnShoot.isEnabled = false
        binding.gameOverLayout.visibility = View.VISIBLE
        binding.tvFinalScore.text = "Final Score: $score"
        clearBees()
    }

    private fun spawnWave() {
        if (binding.gameOverLayout.visibility == View.VISIBLE) return

        for (i in 0 until 3) {
            spawnBee()
        }
    }

    private fun spawnBee() {
        // Create a voxel bee (a cube)
        // Position it randomly within a 3m radius in front of the camera

        val angle = random.nextFloat() * Math.PI * 2
        val distance = random.nextFloat() * (SPAWN_DISTANCE - 1.0f) + 1.0f // 1m to 3m
        val height = (random.nextFloat() * 2.0f) - 1.0f // -1m to +1m height

        val x = (Math.cos(angle) * distance).toFloat()
        val z = -(Math.sin(angle) * distance).toFloat() // Negative Z is forward in Sceneview
        val y = height

        val camera = binding.sceneView.cameraNode
        val spawnPosition = Position(
            x = camera.position.x + x,
            y = camera.position.y + y,
            z = camera.position.z + z
        )

        val engine = binding.sceneView.engine
        val materialLoader = binding.sceneView.materialLoader

        // Let's create a yellow cube
        val beeNode = CubeNode(
            engine = engine,
            size = io.github.sceneview.math.Size(0.2f, 0.2f, 0.2f),
            center = Position(0.0f, 0.0f, 0.0f),
            materialInstance = materialLoader.createColorInstance(android.graphics.Color.YELLOW)
        ).apply {
            position = spawnPosition
        }

        binding.sceneView.addChildNode(beeNode)
        bees.add(beeNode)

        // Simple floating animation
        val startY = beeNode.position.y
        val animator = ValueAnimator.ofFloat(startY - 0.2f, startY + 0.2f).apply {
            duration = 2000L + random.nextInt(1000).toLong()
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val animatedValue = animation.animatedValue as Float
                beeNode.position = Position(beeNode.position.x, animatedValue, beeNode.position.z)
            }
            start()
        }
        animators.add(animator)
    }

    private fun clearBees() {
        for (animator in animators) {
            animator.cancel()
        }
        animators.clear()

        for (bee in bees) {
            binding.sceneView.removeChildNode(bee)
            bee.destroy()
        }
        bees.clear()
    }

    private fun shoot() {
        val screenCenter = dev.romainguy.kotlin.math.Float2(
            binding.sceneView.width / 2.0f,
            binding.sceneView.height / 2.0f
        )

        // Use collision system hitTest against its rendering
        val hitResults = binding.sceneView.collisionSystem.hitTest(screenCenter.x, screenCenter.y)

        if (hitResults.isNotEmpty()) {
            val hitResult = hitResults.first()
            // Check if we hit a bee
            val hitNode = hitResult.node
            if (bees.contains(hitNode)) {
                popBee(hitNode)
            }
        }
    }

    private fun popBee(beeNode: Node) {
        // Find animator and cancel
        val index = bees.indexOf(beeNode)
        if (index != -1 && index < animators.size) {
            animators[index].cancel()
            animators.removeAt(index)
        }
        bees.remove(beeNode)

        // Juice: Play sound
        if (isSoundLoaded) {
            soundPool.play(zapSoundId, 1f, 1f, 1, 0, 1f)
        }

        // Juice: Haptics
        triggerHaptics()

        // Juice: Scale down animation
        val scaleDown = ValueAnimator.ofFloat(beeNode.scale.x, 0.01f).apply {
            duration = 150L
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                beeNode.scale = io.github.sceneview.math.Scale(scale, scale, scale)
            }
        }

        scaleDown.start()

        // Remove node after animation
        binding.sceneView.postDelayed({
            binding.sceneView.removeChildNode(beeNode)
            beeNode.destroy()
        }, 150L)

        score++
        updateScore()

        // Spawn a new wave if all bees are dead
        if (bees.isEmpty()) {
            spawnWave()
        }
    }

    private fun triggerHaptics() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    private fun updateScore() {
        binding.tvScore.text = "Score: $score"
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        soundPool.release()
        for (animator in animators) {
            animator.cancel()
        }
    }
}
