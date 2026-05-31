package dev.vikingsen.absclientapp.core.model

data class HomeShelf(
    val id: String,
    val libraryId: String,
    val label: String,
    val total: Int,
    val type: String,
    val items: List<HomeShelfItem>
)

data class HomeShelfItem(
    val entityId: String,
    val title: String?,
    val subtitle: String?,
    val imageUrl: String?,
    val additionalData: String? = null
)
