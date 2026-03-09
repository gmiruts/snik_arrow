package com.example.snik_arrow.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.snik_arrow.game.models.GameState
import com.example.snik_arrow.game.models.GameStatus
import com.example.snik_arrow.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GameScreen(
    gameState: GameState,
    onStart: () -> Unit,
    onShoot: () -> Unit,
    onReset: () -> Unit,
    onRestartGame: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBlueBackground, DarkBlueVariant)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null // no ripple
            ) {
                if (gameState.status == GameStatus.PLAYING) {
                    onShoot()
                } else if (gameState.status == GameStatus.IDLE) {
                    onStart()
                }
            }
    ) {
        // Game Canvas
        GameCanvas(gameState = gameState, modifier = Modifier.fillMaxSize())

        // Top UI layer
        TopBarUi(
            score = gameState.currentScore,
            level = gameState.level.number,
            onPauseClick = { /* Pause logic */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 24.dp, end = 24.dp)
        )

        // Bottom Chevron Animation
        if (gameState.status == GameStatus.PLAYING) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedChevrons()
                }
            }
        }

        // Overlay Menus
        when (gameState.status) {
            GameStatus.IDLE -> OverlayMenu("TAP TO START")
            GameStatus.GAME_OVER -> OverlayGameOver(
                score = gameState.currentScore,
                onRetry = {
                    onReset()
                    onStart()
                }
            )
            GameStatus.LEVEL_COMPLETE -> OverlayLevelComplete(
                level = gameState.level.number,
                score = gameState.currentScore,
                onNext = {
                    onStart()
                }
            )
            GameStatus.GAME_WON -> OverlayGameWon(
                score = gameState.currentScore,
                onPlayAgain = {
                    onRestartGame()
                    onStart()
                }
            )
            GameStatus.PLAYING -> { /* no overlay */ }
        }
    }
}

@Composable
fun GameCanvas(gameState: GameState, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val maxH = maxHeight
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerPos = Offset(size.width / 2, size.height * 0.4f)
            val densityScale = density 
            
            val targetRadius = 120f * (densityScale * 0.7f)
            val arrowLength = 160f * (densityScale * 0.7f)
            val arrowHeadRadius = 12f * (densityScale * 0.7f)
            val strokeW = 4.dp.toPx()

            // Draw Attached Arrows
            withTransform({
                rotate(degrees = gameState.targetRotation, pivot = centerPos)
            }) {
                gameState.attachedArrows.forEach { arrow ->
                    val angleRad = Math.toRadians(arrow.angle.toDouble())
                    
                    val start = Offset(
                        x = centerPos.x + targetRadius * cos(angleRad).toFloat(),
                        y = centerPos.y + targetRadius * sin(angleRad).toFloat()
                    )
                    val end = Offset(
                        x = centerPos.x + (targetRadius + arrowLength) * cos(angleRad).toFloat(),
                        y = centerPos.y + (targetRadius + arrowLength) * sin(angleRad).toFloat()
                    )
                    
                    val arrowColor = if (arrow.isCollided) Color.Red else NeonCyan
                    
                    drawLine(
                        color = arrowColor,
                        start = start,
                        end = end,
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = arrowColor,
                        radius = arrowHeadRadius,
                        center = end
                    )
                }
            }

            // Draw Target Outer Glow and Core
            drawCircle(
                color = NeonOrange.copy(alpha = 0.3f),
                radius = targetRadius + 12.dp.toPx(),
                center = centerPos
            )
            drawCircle(
                color = NeonOrange,
                radius = targetRadius,
                center = centerPos,
                style = Stroke(width = 6.dp.toPx())
            )
            drawCircle(
                color = NeonOrange.copy(alpha = 0.2f),
                radius = targetRadius,
                center = centerPos
            )

            // Draw Shooting Arrows
            gameState.shootingArrows.forEach { arrow ->
                val yPosFromCenter = arrow.y * (densityScale * 0.7f)
                
                val head = Offset(centerPos.x, centerPos.y + yPosFromCenter)
                val tail = Offset(centerPos.x, centerPos.y + yPosFromCenter + arrowLength)
                
                drawLine(
                    color = NeonCyan,
                    start = head,
                    end = tail,
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = NeonCyan,
                    radius = arrowHeadRadius,
                    center = tail
                )
            }
        }

        // Draw remaining arrows text centered in the target
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = maxH * 0.4f - 24.dp), 
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = gameState.arrowsLeftToShoot.toString(),
                color = TextWhite,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TopBarUi(score: Int, level: Int, onPauseClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "LEVEL $level",
                color = NeonPink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            )
            Text(
                text = "SCORE: $score",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        IconButton(onClick = onPauseClick) {
            Icon(
                imageVector = Icons.Rounded.Pause,
                contentDescription = "Pause",
                tint = NeonCyan,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
fun AnimatedChevrons() {
    val infiniteTransition = rememberInfiniteTransition(label = "ChevronTransition")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ChevronAlpha"
    )

    Column(verticalArrangement = Arrangement.spacedBy((-16).dp)) {
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            tint = NeonPink.copy(alpha = alpha * 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            tint = NeonPink.copy(alpha = alpha * 0.7f),
            modifier = Modifier.size(48.dp)
        )
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowUp,
            contentDescription = null,
            tint = NeonPink.copy(alpha = alpha),
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
fun OverlayMenu(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = NeonPink,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OverlayGameOver(score: Int, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "GAME OVER",
                color = NeonPink,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "FINAL SCORE: $score",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(text = "RETRY", color = DarkBlueBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun OverlayLevelComplete(level: Int, score: Int, onNext: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "LEVEL $level CLEARED!",
                color = NeonCyan,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "SCORE: $score",
                color = TextWhite,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
            ) {
                Text(text = "NEXT LEVEL", color = DarkBlueBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun OverlayGameWon(score: Int, onPlayAgain: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "YOU WIN!",
                color = NeonOrange,
                fontSize = 56.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "ALL 50 LEVELS CLEARED",
                color = NeonCyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "FINAL SCORE: $score",
                color = TextWhite,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Button(
                onClick = onPlayAgain,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
            ) {
                Text(text = "PLAY AGAIN", color = DarkBlueBackground, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
    }
}
