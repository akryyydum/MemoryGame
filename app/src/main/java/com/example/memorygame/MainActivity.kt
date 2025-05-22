package com.example.memorygame

import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.media.Image
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import android.widget.ImageView.ScaleType
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var gridLayout: GridLayout
    private lateinit var startScreen: LinearLayout
    private lateinit var startButton: ImageButton
    private lateinit var leaderboardButton: ImageButton

    private lateinit var gameLayout: LinearLayout
    private lateinit var timerTextView: TextView

    private lateinit var leaderboardScreen: LinearLayout
    private lateinit var leaderboardRecyclerView: RecyclerView
    private lateinit var leaderboardBackButton: Button

    private val cards = mutableListOf<Card>()
    private val cardViews = mutableListOf<AspectRatioImageView>()

    private var firstCard: Card? = null
    private var secondCard: Card? = null
    private var canClick = true

    // Timer variables
    private var timeSeconds = 0
    private var timerRunning = false
    private val timerHandler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null

    private val timerRunnable = object : Runnable {
        override fun run() {
            timeSeconds++
            timerTextView.text = "Time: ${timeSeconds}s"
            timerHandler.postDelayed(this, 1000)
        }
    }

    private val leaderboard = mutableListOf<LeaderboardEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaPlayer = MediaPlayer.create(this, R.raw.background_music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        // Bind views
        gridLayout = findViewById(R.id.gridLayout)
        startScreen = findViewById(R.id.startScreen)
        startButton = findViewById(R.id.startButton)
        leaderboardButton = findViewById(R.id.leaderboardButton)

        gameLayout = findViewById(R.id.gameLayout)
        timerTextView = findViewById(R.id.timerTextView)

        leaderboardScreen = findViewById(R.id.leaderboardScreen)
        leaderboardRecyclerView = findViewById(R.id.leaderboardRecyclerView)
        leaderboardBackButton = findViewById(R.id.leaderboardBackButton)

        // Setup RecyclerView for leaderboard
        leaderboardRecyclerView.layoutManager = LinearLayoutManager(this)

        loadLeaderboard()

        // Start screen visible, others hidden
        showStartScreen()

        startButton.setOnClickListener {
            // Disable the button during animation
            startButton.isEnabled = false

            // Animate the startScreen to fade out smoothly (alpha from 1 to 0)
            startScreen.animate()
                .alpha(0f)
                .setDuration(600) // 600ms fade-out duration
                .withEndAction {
                    // After fade-out ends, hide startScreen completely
                    startScreen.visibility = View.GONE
                    startScreen.alpha = 1f // reset alpha for next time

                    // Show gameLayout with fade-in effect
                    gameLayout.alpha = 0f
                    gameLayout.visibility = View.VISIBLE
                    gameLayout.animate()
                        .alpha(1f)
                        .alpha(1f)
                        .setDuration(600)
                        .withEndAction {
                            // Start the game only after animation finishes
                            startGame()
                            startButton.isEnabled = true
                        }
                        .start()
                }
                .start()
        }


        leaderboardButton.setOnClickListener {
            showLeaderboardScreen()
        }

        leaderboardBackButton.setOnClickListener {
            showStartScreen()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }


    private fun showStartScreen() {
        startScreen.visibility = View.VISIBLE
        gameLayout.visibility = View.GONE
        leaderboardScreen.visibility = View.GONE
    }

    private fun showGameScreen() {
        startScreen.visibility = View.GONE
        gameLayout.visibility = View.VISIBLE
        leaderboardScreen.visibility = View.GONE
    }

    private fun showLeaderboardScreen() {
        startScreen.visibility = View.GONE
        gameLayout.visibility = View.GONE
        leaderboardScreen.visibility = View.VISIBLE
        updateLeaderboardView()
    }

    private fun startGame() {
        cards.clear()
        cardViews.clear()
        gridLayout.removeAllViews()

        timeSeconds = 0
        timerTextView.text = "Time: 0s"
        startTimer()

        val images = listOf(
            R.drawable.card_1, R.drawable.card_2, R.drawable.card_3, R.drawable.card_4,
            R.drawable.card_5, R.drawable.card_6, R.drawable.card_7, R.drawable.card_8
        )

        val shuffledImages = (images + images).shuffled()

        for (i in shuffledImages.indices) {
            cards.add(Card(i, shuffledImages[i]))
        }

        for (i in cards.indices) {
            val imageView = AspectRatioImageView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 30
                    height = (40 * resources.displayMetrics.density).toInt()
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(8, 8, 8, 8)
                }
                setImageResource(R.drawable.card_back)
                scaleType = ScaleType.CENTER_CROP
                setOnClickListener { onCardClick(i) }
                alpha = 0f  // Start fully transparent for animation

                // Add shadow/elevation
                elevation = 8 * resources.displayMetrics.density  // ~8dp elevation for shadow

                // Optional: if you want a shadow outline background to enhance shadow
                // setBackgroundResource(R.drawable.card_shadow_background)
            }
            cardViews.add(imageView)
            gridLayout.addView(imageView)
        }


        firstCard = null
        secondCard = null
        canClick = false  // Disable clicks during animation

        // Animate cards fading in one by one
        val animationDuration = 300L
        val animationDelay = 100L

        for (i in cardViews.indices) {
            val cardView = cardViews[i]
            cardView.animate()
                .alpha(1f)
                .setDuration(animationDuration)
                .setStartDelay(i * animationDelay)
                .withEndAction {
                    // Enable clicks only after the last card's animation finishes
                    if (i == cardViews.size - 1) {
                        canClick = true
                    }
                }
                .start()
        }
    }


    private fun startTimer() {
        timerRunning = true
        timerHandler.postDelayed(timerRunnable, 1000)
    }

    private fun stopTimer() {
        timerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun flipCard(
        view: AspectRatioImageView,
        frontImageResId: Int,
        showFront: Boolean,
        onAnimationEnd: (() -> Unit)? = null
    ) {
        val flipOut = ObjectAnimator.ofFloat(view, "rotationY", 0f, 90f).setDuration(150)
        val flipIn = ObjectAnimator.ofFloat(view, "rotationY", -90f, 0f).setDuration(150)

        flipOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                if (showFront) {
                    view.setImageResource(frontImageResId)
                } else {
                    view.setImageResource(R.drawable.card_back)
                }
                flipIn.start()
            }
        })

        flipIn.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                onAnimationEnd?.invoke()
            }
        })

        flipOut.start()
    }

    private fun onCardClick(index: Int) {
        val card = cards[index]
        if (card.isFaceUp || card.isMatched || !canClick) return

        card.isFaceUp = true
        flipCard(cardViews[index], card.imageResId, true)

        if (firstCard == null) {
            firstCard = card
        } else if (secondCard == null) {
            secondCard = card
            canClick = false
            Handler(Looper.getMainLooper()).postDelayed({ checkMatch() }, 1000)
        }
    }
    private fun checkMatch() {
        if (firstCard != null && secondCard != null) {
            if (firstCard!!.imageResId == secondCard!!.imageResId) {
                firstCard!!.isMatched = true
                secondCard!!.isMatched = true

                reshuffleUnmatchedCards()  // 👈 Shuffle remaining unmatched cards

                checkWin()
            } else {
                firstCard!!.isFaceUp = false
                secondCard!!.isFaceUp = false

                val firstIndex = cards.indexOf(firstCard)
                val secondIndex = cards.indexOf(secondCard)

                flipCard(cardViews[firstIndex], R.drawable.card_back, false)
                flipCard(cardViews[secondIndex], R.drawable.card_back, false)
            }
        }

        firstCard = null
        secondCard = null
        canClick = true
    }

    private fun reshuffleUnmatchedCards() {
        val unmatchedIndices = cards.indices.filter { !cards[it].isMatched }

        // Step 1: Fade out all unmatched cards
        for (i in unmatchedIndices) {
            cardViews[i].animate()
                .alpha(0f)
                .setDuration(300)
                .start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            // Step 2: Reshuffle only unmatched cards
            val unmatchedCards = unmatchedIndices.map { cards[it] }
            val shuffledUnmatched = unmatchedCards.shuffled()

            for ((j, i) in unmatchedIndices.withIndex()) {
                val originalView = cardViews[i]
                val newCard = shuffledUnmatched[j]

                // Find target index of where this card *would* go (visually)
                val targetIndex = cards.indexOfFirst { it.id == newCard.id }
                val targetView = cardViews[targetIndex]

                // Swap position visually using translation animation
                val deltaX = targetView.x - originalView.x
                val deltaY = targetView.y - originalView.y

                originalView.animate()
                    .translationXBy(deltaX)
                    .translationYBy(deltaY)
                    .setDuration(300)
                    .start()

                cards[i] = newCard.copy(id = i)
                originalView.setImageResource(R.drawable.card_back)
            }


            // Step 3: Fade them back in after reshuffling
            for (i in unmatchedIndices) {
                cardViews[i].animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }

        }, 350) // Wait for fade-out to complete
    }

    private fun checkWin() {
        if (cards.all { it.isMatched }) {
            stopTimer()
            Toast.makeText(this, "🎉 You Win! Time: ${timeSeconds}s", Toast.LENGTH_LONG).show()
            promptForName()
        }
    }

    private fun promptForName() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter Your Name")

        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, _ ->
            val name = input.text.toString().trim().ifEmpty { "Anonymous" }
            saveScore(name, timeSeconds)
            dialog.dismiss()
            showLeaderboardScreen()
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
            showStartScreen()
        }

        builder.show()
    }

    private fun saveScore(name: String, time: Int) {
        leaderboard.add(LeaderboardEntry(name, time))
        leaderboard.sortBy { it.timeSeconds }
        if (leaderboard.size > 10) leaderboard.removeAt(leaderboard.lastIndex)  // Keep top 10

        saveLeaderboard()
    }

    private fun saveLeaderboard() {
        val sharedPref = getSharedPreferences("memory_game_prefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        val jsonArray = JSONArray()
        for (entry in leaderboard) {
            val obj = JSONObject()
            obj.put("name", entry.playerName)
            obj.put("time", entry.timeSeconds)
            jsonArray.put(obj)
        }
        editor.putString("leaderboard", jsonArray.toString())
        editor.apply()
    }

    private fun loadLeaderboard() {
        leaderboard.clear()
        val sharedPref = getSharedPreferences("memory_game_prefs", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("leaderboard", null)
        if (jsonString != null) {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.getString("name")
                val time = obj.getInt("time")
                leaderboard.add(LeaderboardEntry(name, time))
            }
        }
    }

    private fun updateLeaderboardView() {
        leaderboardRecyclerView.adapter = LeaderboardAdapter(leaderboard)
    }
}


// RecyclerView Adapter for leaderboard
class LeaderboardAdapter(private val items: List<LeaderboardEntry>) :
    RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankTextView: TextView = view.findViewById(R.id.rankTextView)
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)

    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.leaderboard_item, parent, false)

        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder,position: Int) {
        val entry = items[position]
        holder.rankTextView.text = "${position + 1}. ${entry.playerName}"
        holder.timeTextView.text = "Time: ${entry.timeSeconds}s"
    }
}

