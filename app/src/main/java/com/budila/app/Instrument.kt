package com.budila.app

import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * «Инструмент» — фирменный дизайн Budila в стиле Okto: инженерный калькулятор (Braun ET66 / HP-15C).
 * Фон — корпус, всё нажимаемое — клавиши, всё, что показывает время, — утопленный дисплей
 * с моноширинными цифрами и «призрачными» восьмёрками. Красный — только тревога и звонящий будильник.
 */

/** Цвета одной темы «Инструмента»: корпус, клавиши, дисплей и сигнальный красный. */
@Immutable
data class Instrument(
    val dark: Boolean,
    val bg: Color, val ink: Color, val muted: Color, val line: Color, val soft: Color,
    val key: Color, val keyInk: Color, val keyEdge: Color, val keyHi: Color,
    val well: Color, val wellInk: Color, val wellDim: Color, val wellGhost: Color, val wellEdge: Color,
    val red: Color,
    /** Главная клавиша — инверсия чернил; у цветных тем свой цвет */
    val primary: Color = ink,
    val onPrimary: Color = bg,
)

private fun c(rgb: Long) = Color(0xFF000000 or rgb)
private fun c(rgb: Long, alpha: Float) = c(rgb).copy(alpha = alpha)

private val LightColors = Instrument(
    false, c(0xffffff), c(0x141414), c(0x5f5f5c), c(0xe4e4e1), c(0xf3f3f1),
    c(0xffffff), c(0x141414), c(0xcfcfcc), Color.White,
    c(0xf1f1ef), c(0x111111), c(0x5a5a57), c(0, 0.06f), c(0, 0.13f),
    c(0xd33a3f),
)
private val DarkColors = Instrument(
    true, c(0x141414), c(0xededed), c(0x8e8e8e), c(0x2a2a2a), c(0x1d1d1d),
    c(0x262626), c(0xededed), c(0x070707), c(0xffffff, 0.07f),
    c(0x0a0a0a), c(0xf2f2f2), c(0x8a8a8a), c(0xffffff, 0.06f), c(0, 0.6f),
    c(0xef5a5f),
)
private val OledColors = Instrument(
    true, c(0x000000), c(0xf4f4f4), c(0x8d8d8d), c(0x1d1d1d), c(0x0d0d0d),
    c(0x111111), c(0xf4f4f4), c(0x000000), c(0xffffff, 0.09f),
    c(0x000000), c(0xffffff), c(0x8d8d8d), c(0xffffff, 0.06f), c(0x262626),
    c(0xff5a5f),
)
private val PaperColors = Instrument(
    false, c(0xe9e2d1), c(0x2a251b), c(0x645a49), c(0xd6ccb6), c(0xdfd6c2),
    c(0xf6f0e1), c(0x2a251b), c(0xc2b69c), c(0xffffff, 0.8f),
    c(0x2b2820), c(0xf1e6c8), c(0xa69b80), c(0xf1e6c8, 0.07f), c(0, 0.45f),
    c(0xd33a3f),
)
private val MintColors = Instrument(
    false, c(0xe1eae3), c(0x14241a), c(0x4a5f52), c(0xc9d8cd), c(0xd6e2d9),
    c(0xf1f6f2), c(0x14241a), c(0xb2c4b6), c(0xffffff, 0.8f),
    c(0xcbd9c4), c(0x142313), c(0x415539), c(0x142313, 0.08f), c(0, 0.2f),
    c(0xd33a3f),
)
private val SakuraColors = Instrument(
    false, c(0xfbeff1), c(0x2b1a1f), c(0x7d5e66), c(0xefd6dc), c(0xf6e2e7),
    c(0xfff8f9), c(0x2b1a1f), c(0xe2bfc7), c(0xffffff, 0.9f),
    c(0xffffff), c(0x2b1a1f), c(0x96707a), c(0xd6336c, 0.08f), c(0x963c5a, 0.14f),
    c(0xd6336c), primary = c(0xd6336c), onPrimary = Color.White,
)
private val MidnightColors = Instrument(
    true, c(0x0f141d), c(0xe2e7f0), c(0x8590a6), c(0x212a39), c(0x161d29),
    c(0x1b2331), c(0xe2e7f0), c(0x060a10), c(0xffffff, 0.06f),
    c(0x070a0f), c(0xdfe7f5), c(0x7a879c), c(0xdfe7f5, 0.06f), c(0, 0.65f),
    c(0xf0646a),
)
private val OceanColors = Instrument(
    true, c(0x0b1a22), c(0xe2f1f5), c(0x7d9ba6), c(0x1a2e38), c(0x0f222c),
    c(0x143039), c(0xe2f1f5), c(0x03090c), c(0xffffff, 0.06f),
    c(0x06121a), c(0x5fe0ef), c(0x5d8792), c(0x5fe0ef, 0.08f), c(0, 0.6f),
    c(0xff6b70), primary = c(0x2ec4d6), onPrimary = c(0x04222a),
)
private val NordColors = Instrument(
    true, c(0x2e3440), c(0xeceff4), c(0xa3acbd), c(0x3b4252), c(0x333a47),
    c(0x3b4252), c(0xeceff4), c(0x20242c), c(0xffffff, 0.07f),
    c(0x262b35), c(0x88c0d0), c(0x7f8a9e), c(0x88c0d0, 0.09f), c(0, 0.4f),
    c(0xbf616a), primary = c(0x88c0d0), onPrimary = c(0x1f2530),
)
private val CrimsonColors = Instrument(
    true, c(0x0a0a0a), c(0xf2eeee), c(0x8f8787), c(0x261a1a), c(0x120d0d),
    c(0x1a1313), c(0xf2eeee), c(0x000000), c(0xffffff, 0.06f),
    c(0x000000), c(0xff3b3f), c(0x8a5a5b), c(0xff3b3f, 0.09f), c(0x2a1515),
    c(0xff3b3f), primary = c(0xe5262d), onPrimary = Color.White,
)
private val AmberColors = Instrument(
    true, c(0x0d0b07), c(0xf3e7cf), c(0x9a8a6a), c(0x2a2416), c(0x15120b),
    c(0x1d1910), c(0xf3e7cf), c(0x000000), c(0xffffff, 0.05f),
    c(0x050402), c(0xffb000), c(0x8c6d2c), c(0xffb000, 0.08f), c(0x2b2210),
    c(0xff6a3d), primary = c(0xffb000), onPrimary = c(0x1a1200),
)

/** Темы «Инструмента» — те же, что в Okto. «Классика» следует выбору «Светлая / Тёмная» и чёрному фону. */
enum class InstrumentTheme(@StringRes val title: Int) {
    CLASSIC(R.string.itheme_classic),
    PAPER(R.string.itheme_paper),
    MINT(R.string.itheme_mint),
    SAKURA(R.string.theme_sakura),
    MIDNIGHT(R.string.itheme_midnight),
    OCEAN(R.string.theme_ocean),
    NORD(R.string.itheme_nord),
    CRIMSON(R.string.itheme_crimson),
    AMBER(R.string.itheme_amber),
    ;

    fun palette(dark: Boolean, amoled: Boolean): Instrument = when (this) {
        CLASSIC -> if (!dark) LightColors else if (amoled) OledColors else DarkColors
        PAPER -> PaperColors
        MINT -> MintColors
        SAKURA -> SakuraColors
        MIDNIGHT -> MidnightColors
        OCEAN -> OceanColors
        NORD -> NordColors
        CRIMSON -> CrimsonColors
        AMBER -> AmberColors
    }
}

val LocalInstrument = staticCompositionLocalOf { LightColors }

/** Палитра Material 3 из цветов «Инструмента» — для шторок, полей ввода и прочих стандартных частей. */
fun Instrument.toColorScheme(): ColorScheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
    primary = primary, onPrimary = onPrimary,
    primaryContainer = soft, onPrimaryContainer = ink,
    secondary = ink, onSecondary = bg, secondaryContainer = soft, onSecondaryContainer = ink,
    tertiary = ink, onTertiary = bg, tertiaryContainer = soft, onTertiaryContainer = ink,
    background = bg, onBackground = ink,
    surface = bg, onSurface = ink, surfaceVariant = soft, onSurfaceVariant = muted,
    surfaceContainerLowest = bg, surfaceContainerLow = bg, surfaceContainer = soft,
    surfaceContainerHigh = soft, surfaceContainerHighest = key,
    surfaceTint = Color.Transparent,
    inverseSurface = ink, inverseOnSurface = bg, inversePrimary = bg,
    outline = keyEdge, outlineVariant = line,
    error = red, onError = Color.White, errorContainer = soft, onErrorContainer = red,
)

// ---------- Шрифты: JetBrains Mono для цифр и надписей на корпусе, Golos Text для фраз ----------

val MonoFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.SemiBold),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

val GolosFamily = FontFamily(
    Font(R.font.golos_text, FontWeight.Normal),
    Font(R.font.golos_text, FontWeight.Medium),
    Font(R.font.golos_text, FontWeight.SemiBold),
    Font(R.font.golos_text, FontWeight.Bold),
)

val GolosTypography: Typography = Typography().run {
    fun TextStyle.g() = copy(fontFamily = GolosFamily)
    Typography(
        displayLarge.g(), displayMedium.g(), displaySmall.g(),
        headlineLarge.g(), headlineMedium.g(), headlineSmall.g(),
        titleLarge.g(), titleMedium.g(), titleSmall.g(),
        bodyLarge.g(), bodyMedium.g(), bodySmall.g(),
        labelLarge.g(), labelMedium.g(), labelSmall.g(),
    )
}

object IType {
    /** Надпись на корпусе или в дисплее: моно, капслок, разрядка */
    val label = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.12.em)
    val legend = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.1.em)
    /** Второстепенные показания: даты, «через 8 ч» */
    val reading = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, fontFeatureSettings = "tnum")
    fun readout(size: TextUnit) = TextStyle(
        fontFamily = MonoFamily, fontWeight = FontWeight.Bold, fontSize = size,
        lineHeight = size * 0.98f, letterSpacing = (-0.045).em, fontFeatureSettings = "tnum",
    )
    val headline = TextStyle(fontFamily = GolosFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.03).em)
    val title = TextStyle(fontFamily = GolosFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.02).em)
    val body = TextStyle(fontFamily = GolosFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 21.sp)
    val bodyStrong = TextStyle(fontFamily = GolosFamily, fontWeight = FontWeight.Medium, fontSize = 16.5.sp, lineHeight = 22.sp)
    val small = TextStyle(fontFamily = GolosFamily, fontWeight = FontWeight.Normal, fontSize = 13.5.sp, lineHeight = 18.sp)
    val keyText = TextStyle(fontFamily = GolosFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
}

// ---------- Самотест при включении: все сегменты горят, затем цифры встают на место ----------

const val BOOT_DONE = 2_000L
private const val BOOT_OFF = 90L
private const val BOOT_SETTLE = 540L

/** Миллисекунды с запуска экрана (до [BOOT_DONE]); при отключённой анимации — сразу [BOOT_DONE]. */
val LocalBoot = compositionLocalOf<State<Long>> { mutableLongStateOf(BOOT_DONE) }

@Composable
fun rememberBoot(): State<Long> {
    val context = LocalContext.current
    val reduced = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    return produceState(if (reduced) BOOT_DONE else 0L) {
        if (reduced) return@produceState
        val start = withFrameMillis { it }
        while (value < BOOT_DONE) value = (withFrameMillis { it } - start).coerceAtMost(BOOT_DONE)
    }
}

/** Идёт ли ещё самотест (для индикатора питания и сегментов) */
@Composable
fun bootTesting(): Boolean = LocalBoot.current.value in BOOT_OFF until BOOT_SETTLE

private val InstrumentEase = CubicBezierEasing(.16f, 1f, .3f, 1f)

// ---------- Материалы: клавиша (выпуклая) и дисплей (утопленный) ----------

/** Выпуклая клавиша: блик сверху, ребро снизу; нажатая — утоплена на 1 dp с тенью внутри. */
fun Modifier.keyFace(
    colors: Instrument,
    radius: Dp,
    pressed: Boolean,
    face: Color = colors.key,
    drop: Dp = 1.dp,
): Modifier = drawWithCache {
    val r = CornerRadius(radius.toPx())
    val px = 1.dp.toPx()
    val path = Path().apply { addRoundRect(RoundRect(size.toRect(), r)) }
    val inner = Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.18f), Color.Transparent), endY = 3 * px)
    val ambient = Color.Black.copy(alpha = if (colors.dark) 0.35f else 0.12f)
    onDrawBehind {
        if (pressed) {
            // Утопленная клавиша чуть темнее, иначе на белом корпусе её не видно
            drawRoundRect(lerp(face, colors.ink, 0.07f), cornerRadius = r)
            clipPath(path) { drawRect(inner) }
        } else {
            drawRoundRect(ambient, topLeft = Offset(0f, drop.toPx() + px), size = size, cornerRadius = r)
            drawRoundRect(colors.keyEdge, topLeft = Offset(0f, drop.toPx()), size = size, cornerRadius = r)
            drawRoundRect(colors.keyHi, cornerRadius = r)
            drawRoundRect(face, topLeft = Offset(0f, px), size = Size(size.width, size.height - px), cornerRadius = r)
        }
    }
}

/** Утопленный дисплей: тень по верхнему краю, тонкая кромка и блик под нижним краем. */
fun Modifier.wellFace(colors: Instrument, radius: Dp = 8.dp, fill: Color = colors.well): Modifier = drawWithCache {
    val r = CornerRadius(radius.toPx())
    val px = 1.dp.toPx()
    val path = Path().apply { addRoundRect(RoundRect(size.toRect(), r)) }
    val inner = Brush.verticalGradient(listOf(colors.wellEdge, Color.Transparent), endY = 4 * px)
    onDrawBehind {
        drawRoundRect(colors.keyHi, topLeft = Offset(0f, px), size = size, cornerRadius = r)
        drawRoundRect(fill, cornerRadius = r)
        clipPath(path) { drawRect(inner) }
        drawRoundRect(
            colors.wellEdge, topLeft = Offset(px / 2, px / 2), size = Size(size.width - px, size.height - px),
            cornerRadius = CornerRadius(r.x - px / 2), style = Stroke(px),
        )
    }
}

/** Клавиша. [latched] — зафиксирована нажатой (выбранный день, текущая тема). */
@Composable
fun Key(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    radius: Dp = 10.dp,
    latched: Boolean = false,
    enabled: Boolean = true,
    face: Color? = null,
    contentColor: Color? = null,
    drop: Dp = 1.dp,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalInstrument.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val down = (pressed && enabled) || latched
    Box(
        modifier
            .offset { IntOffset(0, if (down) 1.dp.roundToPx() else 0) }
            .alpha(if (enabled) 1f else 0.45f)
            .keyFace(colors, radius, down, face ?: colors.key, drop)
            .clip(RoundedCornerShape(radius))
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = contentAlignment,
    ) {
        CompositionLocalProvider(LocalContentColor provides (contentColor ?: colors.keyInk)) { content() }
    }
}

/** Главная клавиша экрана — инверсия чернил. Одна на экран. */
@Composable
fun PrimaryKey(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: (@Composable () -> Unit)? = null) {
    val colors = LocalInstrument.current
    Key(
        onClick, modifier.heightIn(min = 50.dp), enabled = enabled,
        face = colors.primary, contentColor = colors.onPrimary, drop = 2.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(10.dp))
            }
            Text(text, style = IType.keyText)
        }
    }
}

/** Обычная клавиша с надписью. [danger] — красная надпись (удаление). */
@Composable
fun TextKey(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false, icon: (@Composable () -> Unit)? = null) {
    val colors = LocalInstrument.current
    Key(onClick, modifier.heightIn(min = 50.dp), contentColor = if (danger) colors.red else colors.keyInk) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp)) {
            if (icon != null) {
                icon()
                Spacer(Modifier.width(8.dp))
            }
            Text(text, style = IType.keyText)
        }
    }
}

/** Утопленный дисплей с показаниями. */
@Composable
fun Well(
    modifier: Modifier = Modifier,
    radius: Dp = 8.dp,
    padding: PaddingValues = PaddingValues(14.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalInstrument.current
    Column(modifier.wellFace(colors, radius).padding(padding)) {
        CompositionLocalProvider(LocalContentColor provides colors.wellInk) { content() }
    }
}

/** Строка надписей дисплея: слева что это, справа второстепенное показание. */
@Composable
fun LegendRow(left: String, modifier: Modifier = Modifier, right: @Composable RowScope.() -> Unit = {}) {
    val colors = LocalInstrument.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(left.uppercase(), style = IType.label, color = colors.wellDim, maxLines = 1, modifier = Modifier.weight(1f).padding(end = 8.dp))
        right()
    }
}

/**
 * Показание с «призрачными» восьмёрками: под каждой цифрой — незажжённая «8».
 * При запуске проходит самотест; [order] сдвигает его, чтобы дисплеи вставали по очереди.
 */
@Composable
fun Digits(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalInstrument.current.wellInk,
    order: Int = 0,
) {
    val colors = LocalInstrument.current
    val t = LocalBoot.current.value
    val ghost = remember(text) { text.map { if (it.isDigit()) '8' else it }.joinToString("") }
    val lit = when {
        t < BOOT_OFF -> null
        t >= BOOT_DONE -> text
        else -> text.mapIndexed { i, ch -> if (ch.isDigit() && t < BOOT_SETTLE + order * 90 + i * 70) '8' else ch }.joinToString("")
    }
    Box(modifier) {
        Text(ghost, style = style, color = colors.wellGhost, maxLines = 1, softWrap = false)
        if (lit != null) Text(lit, style = style, color = color, maxLines = 1, softWrap = false)
    }
}

/** Индикатор: единственные круглые элементы в дизайне. */
@Composable
fun Led(on: Boolean, modifier: Modifier = Modifier, size: Dp = 6.dp, color: Color = LocalInstrument.current.ink) {
    val colors = LocalInstrument.current
    Box(modifier.size(size).background(if (on) color else colors.keyEdge, CircleShape))
}

/** ЖК-сегменты прогресса: [lit] из [count] горят. */
@Composable
fun Segments(count: Int, lit: Int, modifier: Modifier = Modifier, color: Color = LocalInstrument.current.wellInk, height: Dp = 6.dp) {
    val colors = LocalInstrument.current
    val testing = bootTesting()
    Row(modifier.fillMaxWidth().height(height), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(count) { i ->
            val on = i < lit
            Box(
                Modifier.weight(1f).fillMaxHeight().background(
                    when {
                        testing -> colors.wellDim
                        on -> color
                        else -> colors.wellGhost
                    },
                    RoundedCornerShape(1.5.dp),
                ),
            )
        }
    }
}

/** Переключатель: утопленная дорожка и клавиша-ползунок; включён — дорожка заливается чернилами. */
@Composable
fun InstrumentSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, onWell: Boolean = false) {
    val colors = LocalInstrument.current
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, tween(260, easing = InstrumentEase), label = "thumb")
    // Внутри дисплея заливка — цветом его цифр (на «Бумаге» дисплей тёмный, а чернила тоже тёмные)
    val lit = if (onWell) colors.wellInk else colors.ink
    Box(
        modifier
            .size(48.dp, 28.dp)
            .wellFace(colors, 8.dp, if (checked) lit.copy(alpha = 0.85f) else colors.well)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(3.dp),
    ) {
        Box(Modifier.offset(x = x).size(22.dp).keyFace(colors, 6.dp, false))
    }
}

/** Утопленная полоса выбора с выдвижной клавишей; у выбранного варианта горит индикатор. */
@Composable
fun Strip(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalInstrument.current
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .wellFace(colors, 12.dp, colors.soft)
            .padding(3.dp),
    ) {
        val w = maxWidth / options.size
        val x by animateDpAsState(w * selected, tween(260, easing = InstrumentEase), label = "strip")
        Box(Modifier.offset(x = x).width(w).fillMaxHeight().keyFace(colors, 9.dp, false))
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .clickable(role = Role.RadioButton) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
                        if (i == selected) {
                            Led(true, size = 5.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        // Длинная подпись («ГРОМКОСТЬ», «AUSSCHALTEN») уменьшается, а не обрезается
                        BasicText(
                            label.uppercase(),
                            style = IType.legend.copy(fontSize = 11.sp, color = if (i == selected) colors.ink else colors.muted),
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 7.sp, maxFontSize = 11.sp, stepSize = 0.5.sp),
                        )
                    }
                }
            }
        }
    }
}

/** Фишка фильтра: контур на корпусе; выбранная становится клавишей с ярким индикатором. */
@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, count: Int? = null, led: Color? = null) {
    val colors = LocalInstrument.current
    val shape = RoundedCornerShape(8.dp)
    val ledColor = led ?: colors.ink
    Row(
        modifier
            .height(36.dp)
            .then(if (selected) Modifier.keyFace(colors, 8.dp, false) else Modifier.border(1.dp, colors.line, shape))
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .then(if (selected) Modifier.shadow(3.dp, RoundedCornerShape(2.dp), ambientColor = ledColor, spotColor = ledColor) else Modifier)
                .background(ledColor.copy(alpha = if (selected) 1f else 0.55f), RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = IType.body.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = colors.ink, maxLines = 1)
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Text("$count", style = IType.reading.copy(fontSize = 12.sp), color = colors.muted)
        }
    }
}

/** Поле ввода — маленький дисплей с надписью над ним. */
@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    legend: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    /** Модификатор самого поля ввода (например, чтобы поставить в него фокус) */
    inputModifier: Modifier = Modifier,
    /** Красная рамка — введено неверно */
    error: Boolean = false,
) {
    val colors = LocalInstrument.current
    var focused by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(legend.uppercase(), style = IType.label, color = colors.muted)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = MonoFamily, fontWeight = FontWeight.Medium, fontSize = 17.sp, color = colors.wellInk),
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            modifier = inputModifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .wellFace(colors)
                        .then(
                            when {
                                error -> Modifier.border(2.dp, colors.red, RoundedCornerShape(8.dp))
                                focused -> Modifier.border(2.dp, colors.ink, RoundedCornerShape(8.dp))
                                else -> Modifier
                            },
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) Text(placeholder, style = IType.body.copy(fontFamily = MonoFamily, fontSize = 17.sp), color = colors.wellDim)
                    inner()
                }
            },
        )
    }
}

/** Всплывающее сообщение — однострочный дисплей; действие подчёркнуто. */
@Composable
fun Toast(data: SnackbarData, modifier: Modifier = Modifier) {
    val colors = LocalInstrument.current
    Row(
        modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(8.dp), clip = false)
            .wellFace(colors)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(data.visuals.message, style = IType.reading, color = colors.wellInk, modifier = Modifier.weight(1f))
        data.visuals.actionLabel?.let { action ->
            Spacer(Modifier.width(12.dp))
            Text(
                action.uppercase(),
                style = IType.legend.copy(textDecoration = TextDecoration.Underline),
                color = colors.wellInk,
                modifier = Modifier.clickable { data.performAction() }.padding(4.dp),
            )
        }
    }
}

/** Логотип на корпусе: индикатор питания и название. Индикатор загорается после самотеста. */
@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    val colors = LocalInstrument.current
    val on = LocalBoot.current.value >= BOOT_OFF
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Led(on, size = 7.dp)
        Spacer(Modifier.width(8.dp))
        Text("Budila", style = IType.title.copy(fontSize = 19.sp), color = colors.ink)
    }
}

/** Квадратная клавиша с иконкой (настройки, назад). */
@Composable
fun IconKey(onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 44.dp, content: @Composable BoxScope.() -> Unit) {
    Key(onClick, modifier.size(size), content = content)
}

/** Подпись-надпись над элементом управления на корпусе. */
@Composable
fun CasingLegend(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = IType.label, color = LocalInstrument.current.muted, modifier = modifier)
}
