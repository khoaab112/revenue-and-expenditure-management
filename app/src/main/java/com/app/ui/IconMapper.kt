package com.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    fun getIconByName(name: String): ImageVector {
        return when (name) {
            // New Categories added for user request:
            "LocalGasStation" -> Icons.Outlined.LocalGasStation
            "Commute" -> Icons.Outlined.Commute
            "ShoppingCart" -> Icons.Outlined.ShoppingCart
            "Bed" -> Icons.Outlined.Bed
            "MedicalServices" -> Icons.Outlined.MedicalServices
            "Sick" -> Icons.Outlined.Sick
            "Medication" -> Icons.Outlined.Medication
            
            // Existing
            "Restaurant" -> Icons.Outlined.Restaurant
            "DirectionsCar" -> Icons.Outlined.DirectionsCar
            "ShoppingBag" -> Icons.Outlined.ShoppingBag
            "Receipt" -> Icons.Outlined.Receipt
            "SportsEsports" -> Icons.Outlined.SportsEsports
            "School" -> Icons.Outlined.School
            "LocalHospital" -> Icons.Outlined.LocalHospital
            "Home" -> Icons.Outlined.Home
            "Work" -> Icons.Outlined.Work
            "CardGiftcard" -> Icons.Outlined.CardGiftcard
            "Storefront" -> Icons.Outlined.Storefront
            "Payments" -> Icons.Outlined.Payments
            "AccountBalance" -> Icons.Outlined.AccountBalance
            "AccountBalanceWallet" -> Icons.Outlined.AccountBalanceWallet
            "Savings" -> Icons.Outlined.Savings
            "TrendingUp" -> Icons.Outlined.TrendingUp
            "TrendingDown" -> Icons.Outlined.TrendingDown
            "Lock" -> Icons.Outlined.Lock
            "Settings" -> Icons.Outlined.Settings
            
            // Bank
            "Business" -> Icons.Outlined.Business
            "Domain" -> Icons.Outlined.Domain
            "CurrencyExchange" -> Icons.Outlined.CurrencyExchange
            "AssuredWorkload" -> Icons.Outlined.AssuredWorkload
            "SwapHoriz" -> Icons.Outlined.SwapHoriz
            "CorporateFare" -> Icons.Outlined.CorporateFare
            "CreditCard" -> Icons.Outlined.CreditCard

            // Cash
            "Money" -> Icons.Outlined.Money
            "AttachMoney" -> Icons.Outlined.AttachMoney
            "Wallet" -> Icons.Outlined.Wallet
            "PriceCheck" -> Icons.Outlined.PriceCheck
            "LocalAtm" -> Icons.Outlined.LocalAtm
            "PointOfSale" -> Icons.Outlined.PointOfSale

            // E-Wallet
            "PhonelinkRing" -> Icons.Outlined.PhonelinkRing
            "Contactless" -> Icons.Outlined.Contactless
            "QrCode" -> Icons.Outlined.QrCode
            "PhoneAndroid" -> Icons.Outlined.PhoneAndroid
            "Security" -> Icons.Outlined.Security
            "TapAndPlay" -> Icons.Outlined.TapAndPlay
            "Nfc" -> Icons.Outlined.Nfc
            "MobileScreenShare" -> Icons.Outlined.MobileScreenShare

            // Savings
            "Inventory" -> Icons.Outlined.Inventory
            "CurrencyBitcoin" -> Icons.Outlined.CurrencyBitcoin
            "MonetizationOn" -> Icons.Outlined.MonetizationOn
            "Star" -> Icons.Outlined.Star
            "WorkspacePremium" -> Icons.Outlined.WorkspacePremium
            "Redeem" -> Icons.Outlined.Redeem
            
            // New modern & attractive icons
            "Coffee" -> Icons.Outlined.Coffee
            "LocalBar" -> Icons.Outlined.LocalBar
            "Flight" -> Icons.Outlined.Flight
            "Checkroom" -> Icons.Outlined.Checkroom
            "FitnessCenter" -> Icons.Outlined.FitnessCenter
            "Pets" -> Icons.Outlined.Pets
            "ChildCare" -> Icons.Outlined.ChildCare
            "FaceRetouchingNatural" -> Icons.Outlined.FaceRetouchingNatural
            "Spa" -> Icons.Outlined.Spa
            "Movie" -> Icons.Outlined.Movie
            "Theaters" -> Icons.Outlined.Theaters
            "LibraryMusic" -> Icons.Outlined.LibraryMusic
            "Headphones" -> Icons.Outlined.Headphones
            "VideogameAsset" -> Icons.Outlined.VideogameAsset
            "LocalPizza" -> Icons.Outlined.LocalPizza
            "LocalCafe" -> Icons.Outlined.LocalCafe
            "LocalDining" -> Icons.Outlined.LocalDining
            "Brush" -> Icons.Outlined.Brush
            "Palette" -> Icons.Outlined.Palette
            "Computer" -> Icons.Outlined.Computer
            "PhoneIphone" -> Icons.Outlined.PhoneIphone
            "CameraAlt" -> Icons.Outlined.CameraAlt
            "Map" -> Icons.Outlined.Map
            "CrueltyFree" -> Icons.Outlined.CrueltyFree
            "PedalBike" -> Icons.Outlined.PedalBike
            "AutoAwesome" -> Icons.Outlined.AutoAwesome
            "Celebration" -> Icons.Outlined.Celebration
            "Cake" -> Icons.Outlined.Cake
            "EmojiEmotions" -> Icons.Outlined.EmojiEmotions
            "Favorite" -> Icons.Outlined.Favorite
            "Mood" -> Icons.Outlined.Mood
            "SelfImprovement" -> Icons.Outlined.SelfImprovement
            "EmojiObjects" -> Icons.Outlined.EmojiObjects
            "RocketLaunch" -> Icons.Outlined.RocketLaunch
            
            else -> Icons.Outlined.Category
        }
    }
}
