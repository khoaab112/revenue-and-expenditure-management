package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    fun getIconByName(name: String): ImageVector {
        return when (name) {
            "Restaurant" -> Icons.Default.Restaurant
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "ShoppingBag" -> Icons.Default.ShoppingBag
            "Receipt" -> Icons.Default.Receipt
            "SportsEsports" -> Icons.Default.SportsEsports
            "School" -> Icons.Default.School
            "LocalHospital" -> Icons.Default.LocalHospital
            "Home" -> Icons.Default.Home
            "Work" -> Icons.Default.Work
            "CardGiftcard" -> Icons.Default.CardGiftcard
            "Storefront" -> Icons.Default.Storefront
            "Payments" -> Icons.Default.Payments
            "AccountBalance" -> Icons.Default.AccountBalance
            "AccountBalanceWallet" -> Icons.Default.AccountBalanceWallet
            "Savings" -> Icons.Default.Savings
            "TrendingUp" -> Icons.Default.TrendingUp
            "TrendingDown" -> Icons.Default.TrendingDown
            "Lock" -> Icons.Default.Lock
            "Settings" -> Icons.Default.Settings
            
            // New modern & attractive icons
            "Coffee" -> Icons.Default.Coffee
            "LocalBar" -> Icons.Default.LocalBar
            "Flight" -> Icons.Default.Flight
            "Checkroom" -> Icons.Default.Checkroom
            "FitnessCenter" -> Icons.Default.FitnessCenter
            "Pets" -> Icons.Default.Pets
            "ChildCare" -> Icons.Default.ChildCare
            "FaceRetouchingNatural" -> Icons.Default.FaceRetouchingNatural
            "Spa" -> Icons.Default.Spa
            "Movie" -> Icons.Default.Movie
            "Theaters" -> Icons.Default.Theaters
            "LibraryMusic" -> Icons.Default.LibraryMusic
            "Headphones" -> Icons.Default.Headphones
            "VideogameAsset" -> Icons.Default.VideogameAsset
            "LocalPizza" -> Icons.Default.LocalPizza
            "LocalCafe" -> Icons.Default.LocalCafe
            "LocalDining" -> Icons.Default.LocalDining
            "Brush" -> Icons.Default.Brush
            "Palette" -> Icons.Default.Palette
            "Computer" -> Icons.Default.Computer
            "PhoneIphone" -> Icons.Default.PhoneIphone
            "CameraAlt" -> Icons.Default.CameraAlt
            "Map" -> Icons.Default.Map
            "CrueltyFree" -> Icons.Default.CrueltyFree
            "PedalBike" -> Icons.Default.PedalBike
            "AutoAwesome" -> Icons.Default.AutoAwesome
            "Celebration" -> Icons.Default.Celebration
            "Cake" -> Icons.Default.Cake
            "EmojiEmotions" -> Icons.Default.EmojiEmotions
            "Favorite" -> Icons.Default.Favorite
            "Mood" -> Icons.Default.Mood
            "SelfImprovement" -> Icons.Default.SelfImprovement
            "EmojiObjects" -> Icons.Default.EmojiObjects
            "RocketLaunch" -> Icons.Default.RocketLaunch
            
            else -> Icons.Default.Category
        }
    }
}
