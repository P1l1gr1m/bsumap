package com.shist.data.repository.mappers

import com.shist.data.roomDB.entities.buildingItem.buildingItemImage.BuildingItemImageEntityDB
import com.shist.domain.BuildingItemImage

// This mapper converts a database entity to a domain entity
class BuildingItemImageDBMapper {

    fun fromDBToDomain(item: BuildingItemImageEntityDB?) : BuildingItemImage? {
        return if (item == null) {
            null
        } else {
            BuildingItemImage(item.id,
                item.description,
                item.imagePath)
        }
    }

}