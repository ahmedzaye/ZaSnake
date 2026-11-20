package com.ahmedzayed.zasnake.ui

import com.ahmedzayed.zasnake.billing.CoinPriceCatalog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ahmedzayed.zasnake.game.COIN_PACKAGES
import com.ahmedzayed.zasnake.game.AppUiState
import com.ahmedzayed.zasnake.game.GameTheme
import com.ahmedzayed.zasnake.game.SnakeSkin
import com.ahmedzayed.zasnake.game.SnakeHeadStyle
import com.ahmedzayed.zasnake.game.SnakeBodyType
import com.ahmedzayed.zasnake.game.ARAB_COLLECTION
import kotlin.math.min

@Composable
internal fun StyleLabItemCard(
    title: String,
    selected: Boolean,
    owned: Boolean,
    equipped: Boolean,
    price: Int,
    balance: Long,
    language: String,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
    onPreview: () -> Unit,
    onAction: () -> Unit,
    paidBalance: Int? = null,
    paidWalletLoading: Boolean = false,
    legacyOwnershipLoaded: Boolean = false,
    onPaidAction: (() -> Unit)? = null
) {
    val ar = language == "ar"
    Column(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(if (selected) Playform.PaleLavender else Playform.White)
            .border(if (selected) 2.dp else 1.dp,
                if (selected) Playform.Ink else Playform.Border,
                RoundedCornerShape(18.dp))
            .clickable(onClick = onPreview).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(12.dp))
            .background(Playform.Ink)) {
            preview()
            if (selected) {
                Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(20.dp)
                    .clip(CircleShape).background(Playform.Gold),
                    contentAlignment = Alignment.Center) {
                    androidx.compose.material3.Icon(Icons.Rounded.CheckCircle, null,
                        tint = Playform.Ink, modifier = Modifier.size(16.dp))
                }
            }
        }
        Text(title, color = Playform.Ink, fontSize = 13.sp,
            fontWeight = FontWeight.Black, minLines = 1, maxLines = 2)
        Button(
            onClick = onAction,
            enabled = !equipped && (owned || balance >= price),
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Playform.Coral, contentColor = Playform.Ink,
                disabledContainerColor = Playform.PaleLavender,
                disabledContentColor = Playform.TextSecondary)
        ) {
            Text(when {
                equipped -> if (ar) "مُجهّز ✓" else "Equipped ✓"
                owned -> if (ar) "تجهيز" else "Equip"
                else -> "🪙 $price"
            }, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
        if (!owned && price > 0 && paidBalance != null && onPaidAction != null) {
            OutlinedButton(
                onClick = onPaidAction,
                enabled = legacyOwnershipLoaded && paidBalance >= price && !paidWalletLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
            ) {
                Text(if (ar) "شراء برصيد مدفوع" else "Use purchased coins",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun ShopScreen(
    state: AppUiState,
    coinPriceCatalog: CoinPriceCatalog?,
    onBack: () -> Unit,
    onPurchase: (SnakeSkin) -> Boolean,
    onEquip: (SnakeSkin) -> Unit,
    onPurchaseHead: (SnakeHeadStyle) -> Boolean,
    onEquipHead: (SnakeHeadStyle) -> Unit,
    onPurchaseBody: (SnakeBodyType) -> Boolean,
    onEquipBody: (SnakeBodyType) -> Unit,
    onPurchaseTheme: (GameTheme) -> Boolean,
    onApplyTheme: (GameTheme) -> Unit
) {
    val lang = state.settings.language
    val coinPriceState = coinPriceCatalog?.state
    val playPrices = coinPriceCatalog?.localizedPrices.orEmpty()
    val paidBalance = coinPriceCatalog?.paidWalletCoins
    val paidOwned = coinPriceCatalog?.paidOwnedItemIds.orEmpty()
    val paidWalletLoading = coinPriceCatalog?.paidWalletLoading == true
    // Before spending real-money coins, let Play Games finish restoring
    // previous purchases made with earned/free coins on another device.
    // These legacy items remain locally/cloud owned; they are never re-billed
    // or promoted into the paid-only wallet.
    val cloudOwnershipLoaded = state.cloudStatus == "synced"
    val legacyOwnershipLoaded = cloudOwnershipLoaded &&
        coinPriceCatalog?.legacyOwnershipReady == true
    // Only after Play Games has merged existing saves may the app register a
    // persistent, additive spending veto. Never derive paid grants from it.
    val legacyItemIds = (
        state.settings.ownedSkinKeys.filter { key ->
            SnakeSkin.entries.any { !it.isFree && it.key == key }
        }.map { "skin_" + it } +
        state.stats.purchasedSkinKeys.map { "skin_" + it } +
        state.settings.ownedHeadKeys.filter { key ->
            SnakeHeadStyle.entries.any { !it.isFree && it.key == key }
        }.map { "head_" + it } +
        state.stats.purchasedHeadKeys.map { "head_" + it } +
        state.settings.ownedBodyKeys.filter { key ->
            SnakeBodyType.entries.any { !it.isFree && it.key == key }
        }.map { "body_" + it } +
        state.stats.purchasedBodyKeys.map { "body_" + it } +
        state.settings.ownedThemeKeys.filter { key ->
            GameTheme.entries.any { !it.isFree && it.key == key }
        }.map { "theme_" + it } +
        state.stats.purchasedThemeKeys.map { "theme_" + it }
    ).toSet()
    LaunchedEffect(cloudOwnershipLoaded, legacyItemIds, coinPriceCatalog) {
        if (cloudOwnershipLoaded) {
            coinPriceCatalog?.reportLegacyOwnership(legacyItemIds)
        }
    }
    var pendingPaidItem by remember { mutableStateOf<Triple<String, String, Int>?>(null) }
    val prices = SnakeSkin.entries.associateWith { it.priceCoins }
    var previewSkin by remember(state.settings.skin) { mutableStateOf(state.settings.skin) }
    var pendingBuy by remember { mutableStateOf<SnakeSkin?>(null) }
    var tab by remember { mutableStateOf("skins") }
    LaunchedEffect(tab, coinPriceCatalog) {
        // Authenticate on entering the shop, not only on the coin-pack tab.
        coinPriceCatalog?.refresh()
        coinPriceCatalog?.refreshPaidWallet()
    }
    var snakeTab by remember { mutableStateOf("colors") }
    var previewHead by remember(state.settings.headStyle) { mutableStateOf(state.settings.headStyle) }
    var pendingHeadBuy by remember { mutableStateOf<SnakeHeadStyle?>(null) }
    var previewBody by remember(state.settings.bodyType) { mutableStateOf(state.settings.bodyType) }
    var pendingBodyBuy by remember { mutableStateOf<SnakeBodyType?>(null) }
    var previewTheme by remember(state.settings.theme) {
        mutableStateOf(state.settings.theme.takeUnless { it.isFree } ?: GameTheme.OCEAN)
    }
    var pendingThemeBuy by remember { mutableStateOf<GameTheme?>(null) }

    val compactInventory = LocalConfiguration.current.screenWidthDp < 360 ||
        LocalDensity.current.fontScale > 1.25f
    val columns = if (compactInventory) 1 else 2
    Column(Modifier.fillMaxSize().background(Playform.Cream)
        .padding(horizontal = 16.dp, vertical = 12.dp)) {
        ScreenHeader(if (lang == "ar") "عالم التخصيص" else "Style Lab", onBack)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(tx(lang, "shop_skins"), tab == "skins") { tab = "skins" }
            ChoiceChip(tx(lang, "shop_themes"), tab == "themes") { tab = "themes" }
            // Keep paid coin packages visible only in debug, as in release v4.
            if (com.ahmedzayed.zasnake.BuildConfig.DEBUG || com.ahmedzayed.zasnake.BuildConfig.BILLING_INTEGRATION_ENABLED) {
                ChoiceChip(tx(lang, "shop_coins"), tab == "coins") { tab = "coins" }
            }
        }
        Spacer(Modifier.height(5.dp))
        // Compact PLAYFORM preview leaves more room for the real inventory.
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Playform.Ink)
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("LIVE PREVIEW", color = Playform.Gold,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Text("🪙 ${state.stats.coins}", color = Playform.Gold,
                    fontSize = 15.sp, fontWeight = FontWeight.Black)
                if (paidBalance != null) {
                    Text((if (lang == "ar") "مدفوعة: " else "Paid: ") + paidBalance,
                        color = Playform.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (tab != "coins") {
                Box(Modifier.fillMaxWidth()
                    .height(if (LocalConfiguration.current.screenHeightDp < 650) 78.dp else 96.dp)
                    .clip(RoundedCornerShape(14.dp)).background(Color(0xFF07131B))) {
                    if (tab == "skins") when (snakeTab) {
                        "heads" -> HeadStylePreview(previewHead, state.settings.skin,
                            Modifier.fillMaxSize(), state.settings.bodyType)
                        "body" -> BodyStylePreview(previewBody, state.settings.skin,
                            state.settings.headStyle, Modifier.fillMaxSize())
                        else -> SkinPreview(previewSkin, Modifier.fillMaxSize(),
                            state.settings.headStyle, state.settings.bodyType)
                    } else ThemeMiniPreview(previewTheme, state.settings.skin, Modifier.fillMaxSize())
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tab == "skins") {
                item {
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ChoiceChip(tx(lang, "shop_colors"), snakeTab == "colors") { snakeTab = "colors" }
                        ChoiceChip(tx(lang, "shop_heads"), snakeTab == "heads") { snakeTab = "heads" }
                        ChoiceChip(tx(lang, "shop_bodies"), snakeTab == "body") { snakeTab = "body" }
                    }
                }
                if (snakeTab == "colors") {
                    item { PlayformSectionTitle(if (lang == "ar") "ألوان الثعبان" else "Snake Colors") }
                    val normalSkins = listOf(
                        SnakeSkin.GREEN, SnakeSkin.PURPLE, SnakeSkin.YELLOW,
                        SnakeSkin.CYBER, SnakeSkin.ICE, SnakeSkin.FIRE
                    )
                    items(normalSkins.chunked(columns)) { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.forEach { skin ->
                                val owned = skin.isFree || skin.key in state.settings.ownedSkinKeys ||
                                    ("skin_" + skin.key) in paidOwned
                                StyleLabItemCard(tx(lang, skin.key), previewSkin == skin,
                                    owned, state.settings.skin == skin, skin.priceCoins,
                                    state.stats.coins, lang, Modifier.weight(1f),
                                    preview = {
                                        SkinPreview(skin, Modifier.fillMaxSize(),
                                            state.settings.headStyle, state.settings.bodyType)
                                    },
                                    onPreview = { previewSkin = skin },
                                    onAction = {
                                        if (owned) onEquip(skin)
                                        else { previewSkin = skin; pendingBuy = skin }
                                    },
                                    paidBalance = paidBalance, paidWalletLoading = paidWalletLoading,
                                    legacyOwnershipLoaded = legacyOwnershipLoaded,
                                    onPaidAction = {
                                        pendingPaidItem = Triple("skin_" + skin.key,
                                            tx(lang, skin.key), skin.priceCoins)
                                    })
                            }
                            repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    item {
                        PlayformSectionTitle(tx(lang, "arab_collection"),
                            tx(lang, "arab_collection_desc"))
                    }
                    items(ARAB_COLLECTION.chunked(columns)) { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.forEach { skin ->
                                val owned = skin.isFree || skin.key in state.settings.ownedSkinKeys ||
                                    ("skin_" + skin.key) in paidOwned
                                StyleLabItemCard(tx(lang, skin.key), previewSkin == skin,
                                    owned, state.settings.skin == skin, skin.priceCoins,
                                    state.stats.coins, lang, Modifier.weight(1f),
                                    preview = {
                                        SkinPreview(skin, Modifier.fillMaxSize(),
                                            state.settings.headStyle, state.settings.bodyType)
                                    },
                                    onPreview = { previewSkin = skin },
                                    onAction = {
                                        if (owned) onEquip(skin)
                                        else { previewSkin = skin; pendingBuy = skin }
                                    },
                                    paidBalance = paidBalance, paidWalletLoading = paidWalletLoading,
                                    legacyOwnershipLoaded = legacyOwnershipLoaded,
                                    onPaidAction = {
                                        pendingPaidItem = Triple("skin_" + skin.key,
                                            tx(lang, skin.key), skin.priceCoins)
                                    })
                            }
                            repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                } else if (snakeTab == "heads") {
                    item { PlayformSectionTitle(tx(lang, "head_title"), tx(lang, "head_hint")) }
                    items(SnakeHeadStyle.entries.toList().chunked(columns)) { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.forEach { head ->
                                val owned = head.isFree || head.key in state.settings.ownedHeadKeys ||
                                    ("head_" + head.key) in paidOwned
                                StyleLabItemCard(tx(lang, "head_${head.key}"), previewHead == head,
                                    owned, state.settings.headStyle == head, head.priceCoins,
                                    state.stats.coins, lang, Modifier.weight(1f),
                                    preview = {
                                        HeadStylePreview(head, state.settings.skin,
                                            Modifier.fillMaxSize(), state.settings.bodyType)
                                    },
                                    onPreview = { previewHead = head },
                                    onAction = {
                                        if (owned) onEquipHead(head) else pendingHeadBuy = head
                                    },
                                    paidBalance = paidBalance, paidWalletLoading = paidWalletLoading,
                                    legacyOwnershipLoaded = legacyOwnershipLoaded,
                                    onPaidAction = {
                                        pendingPaidItem = Triple("head_" + head.key,
                                            tx(lang, "head_" + head.key), head.priceCoins)
                                    })
                            }
                            repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                } else {
                    item { PlayformSectionTitle(tx(lang, "body_title"), tx(lang, "body_soon_hint")) }
                    items(SnakeBodyType.entries.toList().chunked(columns)) { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            group.forEach { body ->
                                val owned = body.isFree || body.key in state.settings.ownedBodyKeys ||
                                    ("body_" + body.key) in paidOwned
                                StyleLabItemCard(tx(lang, "body_${body.key}"), previewBody == body,
                                    owned, state.settings.bodyType == body, body.priceCoins,
                                    state.stats.coins, lang, Modifier.weight(1f),
                                    preview = {
                                        // Real renderer, including distinct Crystal and Smooth geometry.
                                        BodyStylePreview(body, state.settings.skin,
                                            state.settings.headStyle, Modifier.fillMaxSize())
                                    },
                                    onPreview = { previewBody = body },
                                    onAction = {
                                        if (owned) onEquipBody(body) else pendingBodyBuy = body
                                    },
                                    paidBalance = paidBalance, paidWalletLoading = paidWalletLoading,
                                    legacyOwnershipLoaded = legacyOwnershipLoaded,
                                    onPaidAction = {
                                        pendingPaidItem = Triple("body_" + body.key,
                                            tx(lang, "body_" + body.key), body.priceCoins)
                                    })
                            }
                            repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            } else if (tab == "themes") {
                item { PlayformSectionTitle(tx(lang, "game_themes"), tx(lang, "theme_shop_hint")) }
                items(GameTheme.entries.filterNot { it.isFree }.chunked(columns)) { group ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.forEach { theme ->
                            val owned = theme.key in state.settings.ownedThemeKeys ||
                                ("theme_" + theme.key) in paidOwned
                            StyleLabItemCard(themeTitle(theme, lang), previewTheme == theme,
                                owned, state.settings.theme == theme, theme.priceCoins,
                                state.stats.coins, lang, Modifier.weight(1f),
                                preview = {
                                    ThemeMiniPreview(theme, state.settings.skin, Modifier.fillMaxSize())
                                },
                                onPreview = { previewTheme = theme },
                                onAction = {
                                    if (owned) onApplyTheme(theme)
                                    else pendingThemeBuy = theme
                                },
                                paidBalance = paidBalance, paidWalletLoading = paidWalletLoading,
                                onPaidAction = {
                                    pendingPaidItem = Triple("theme_" + theme.key,
                                        themeTitle(theme, lang), theme.priceCoins)
                                })
                        }
                        repeat(columns - group.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                item { Text(tx(lang, "theme_free_hint"),
                    color = Playform.TextSecondary, fontSize = 12.sp) }
            } else if (tab == "coins") {
                item { PlayformSectionTitle(tx(lang, "coin_shop_title"), tx(lang, "coin_shop_info")) }
                if (coinPriceCatalog != null) {
                    item {
                        PlayformCard {
                            Text(
                                if (paidBalance == null)
                                    if (lang == "ar") "الرصيد المدفوع غير متاح حتى يتصل بالخادم"
                                    else "Paid wallet unavailable until server connection"
                                else if (lang == "ar") "رصيد العملات المدفوعة: " + paidBalance
                                else "Purchased coin balance: " + paidBalance,
                                color = Playform.Ink, fontWeight = FontWeight.Bold
                            )
                            if (coinPriceCatalog.paidWalletStatus.isNotEmpty()) {
                                Text(coinPriceCatalog.paidWalletStatus,
                                    color = Playform.TextSecondary, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { coinPriceCatalog.refreshPaidWallet() },
                                enabled = !paidWalletLoading
                            ) { Text(if (lang == "ar") "تحديث الرصيد" else "Refresh balance") }
                        }
                    }
                }
                if (coinPriceCatalog != null && coinPriceState == CoinPriceCatalog.State.UNAVAILABLE) {
                    item {
                        OutlinedButton(onClick = { coinPriceCatalog.refresh() }) {
                            Text(if (lang == "ar") "إعادة تحميل الأسعار"
                                else "Retry loading prices")
                        }
                    }
                }
                items(COIN_PACKAGES, key = { it.productId }) { pack ->
                    PlayformCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🪙", fontSize = 30.sp)
                            Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                                Text("${pack.coinAmount} ${if (lang == "ar") "عملة" else "coins"}",
                                    color = Playform.Ink, fontWeight = FontWeight.Black,
                                    fontSize = 17.sp)
                                Text(playPrices[pack.productId] ?: when (coinPriceState) {
                                    CoinPriceCatalog.State.LOADING ->
                                        if (lang == "ar") "جارٍ تحميل السعر" else "Loading price"
                                    CoinPriceCatalog.State.UNAVAILABLE ->
                                        if (lang == "ar") "السعر غير متاح" else "Price unavailable"
                                    else -> tx(lang, "coin_shop_price_pending")
                                }, color = Playform.TextSecondary, fontSize = 12.sp)
                            }
                        }
                        Button(onClick = { coinPriceCatalog?.buy(pack.productId) },
                            enabled = coinPriceCatalog?.canBuy(pack.productId) == true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Playform.Coral, contentColor = Playform.Ink)) {
                            Text(if (coinPriceCatalog?.canBuy(pack.productId) == true)
                                if (lang == "ar") "الشراء عبر Google Play" else "Buy with Google Play"
                                else tx(lang, "coin_shop_unavailable"), fontSize = 13.sp)
                        }
                    }
                }
                item {
                    Text(coinPriceCatalog?.statusMessage ?: tx(lang, "coin_shop_no_payment"),
                        color = Playform.TextSecondary, fontSize = 12.sp)
                }
            }
            item { Spacer(Modifier.height(10.dp)) }
        }
    }

    pendingThemeBuy?.let { theme ->
        AlertDialog(
            onDismissRequest = { pendingThemeBuy = null },
            title = { Text(if (lang == "ar") "تأكيد شراء الثيم" else "Confirm theme purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ThemeMiniPreview(theme, state.settings.skin, Modifier.fillMaxWidth().height(100.dp))
                    Text(if (lang == "ar") "شراء ثيم ${themeTitle(theme, lang)} مقابل ${theme.priceCoins} عملة؟ هيتفعّل مباشرة ويبقى متاح في الإعدادات." else
                        "Buy ${themeTitle(theme, lang)} for ${theme.priceCoins} coins? It will be applied immediately and unlocked in Settings.")
                }
            },
            confirmButton = {
                TextButton(onClick = { onPurchaseTheme(theme); pendingThemeBuy = null }) {
                    Text(if (lang == "ar") "تأكيد الشراء" else "Buy now", color = Playform.Ink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingThemeBuy = null }) { Text(tx(lang, "cancel"), color = Muted) } },
            containerColor = Panel, titleContentColor = PrimaryText, textContentColor = PrimaryText
        )
    }

    pendingBodyBuy?.let { body ->
        AlertDialog(
            onDismissRequest = { pendingBodyBuy = null },
            title = { Text(if (lang == "ar") "تأكيد شراء الجسم" else "Confirm body purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BodyStylePreview(body, state.settings.skin, state.settings.headStyle,
                        Modifier.fillMaxWidth().height(100.dp))
                    Text(if (lang == "ar") "شراء ${tx(lang, "body_${body.key}")} مقابل ${body.priceCoins} عملة؟"
                        else "Buy ${tx(lang, "body_${body.key}")} for ${body.priceCoins} coins?", color = PrimaryText)
                }
            },
            confirmButton = { TextButton(onClick = { onPurchaseBody(body); pendingBodyBuy = null }) {
                Text(if (lang == "ar") "تأكيد الشراء" else "Buy now", color = Playform.Ink)
            } },
            dismissButton = { TextButton(onClick = { pendingBodyBuy = null }) {
                Text(tx(lang, "cancel"), color = Muted)
            } },
            containerColor = Panel, titleContentColor = PrimaryText
        )
    }

    pendingHeadBuy?.let { head ->
        AlertDialog(
            onDismissRequest = { pendingHeadBuy = null },
            title = { Text(if (lang == "ar") "تأكيد شراء الرأس" else "Confirm head purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth().height(110.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF07131B))) {
                        HeadStylePreview(head, state.settings.skin, Modifier.fillMaxSize(), state.settings.bodyType)
                    }
                    Text(if (lang == "ar") "شراء رأس ${tx(lang, "head_${head.key}")} مقابل ${head.priceCoins} عملة؟"
                        else "Buy ${tx(lang, "head_${head.key}")} for ${head.priceCoins} coins?", color = PrimaryText)
                }
            },
            confirmButton = { TextButton(onClick = { onPurchaseHead(head); pendingHeadBuy = null }) {
                Text(if (lang == "ar") "تأكيد الشراء" else "Buy now", color = Playform.Ink)
            } },
            dismissButton = { TextButton(onClick = { pendingHeadBuy = null }) {
                Text(tx(lang, "cancel"), color = Muted)
            } },
            containerColor = Panel, titleContentColor = PrimaryText
        )
    }

    pendingBuy?.let { skin ->
        val price = prices.getValue(skin)
        AlertDialog(
            onDismissRequest = { pendingBuy = null },
            title = { Text(if (lang == "ar") "تأكيد الشراء" else "Confirm purchase", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF07131B))) {
                        SkinPreview(skin, Modifier.fillMaxSize(), state.settings.headStyle, state.settings.bodyType)
                    }
                    Text(if (lang == "ar") "شراء شكل ${tx(lang, skin.key)} مقابل $price عملة؟ سيتم تجهيز الشكل مباشرة." else "Buy ${tx(lang, skin.key)} for $price coins? It will be equipped right away.")
                }
            },
            confirmButton = {
                TextButton(onClick = { onPurchase(skin); pendingBuy = null }) {
                    Text(if (lang == "ar") "تأكيد الشراء" else "Buy now", color = Playform.Ink, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingBuy = null }) { Text(if (lang == "ar") "إلغاء" else "Cancel", color = Muted) } },
            containerColor = Panel,
            titleContentColor = PrimaryText,
            textContentColor = PrimaryText
        )
    }

    pendingPaidItem?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingPaidItem = null },
            title = { Text(if (lang == "ar") "تأكيد استخدام العملات المدفوعة"
                else "Confirm purchased-coin spend") },
            text = {
                Text(
                    if (lang == "ar")
                        "شراء " + pending.second + " مقابل " + pending.third +
                            " عملة مدفوعة؟ سيؤكد الخادم عملية الشراء قبل فتح العنصر."
                    else "Buy " + pending.second + " for " + pending.third +
                        " purchased coins? The server must confirm ownership first."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = paidBalance != null && paidBalance >= pending.third &&
                        !paidWalletLoading && legacyOwnershipLoaded,
                    onClick = {
                        coinPriceCatalog?.buyPaidItem(pending.first)
                        pendingPaidItem = null
                    }
                ) { Text(if (lang == "ar") "تأكيد الصرف" else "Spend purchased coins") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPaidItem = null }) {
                    Text(if (lang == "ar") "إلغاء" else "Cancel")
                }
            }
        )
    }
}
