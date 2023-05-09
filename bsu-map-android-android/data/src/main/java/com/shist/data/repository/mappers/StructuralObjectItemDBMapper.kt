package com.shist.data.repository.mappers

import com.shist.data.roomDB.entities.buildingItem.structuralObjectItem.StructuralObjectItemEntityDB
import com.shist.data.roomDB.entities.buildingItem.structuralObjectItem.iconItem.IconItemEntityDB
import com.shist.domain.StructuralObjectItem

// This mapper converts a database entity to a domain entity
class StructuralObjectItemDBMapper {

    fun fromDBToDomain(itemSO: StructuralObjectItemEntityDB?, itemI: IconItemEntityDB?) : StructuralObjectItem? {
        return if (itemSO == null) {
            null
        } else {
            StructuralObjectItem(itemSO.id,
                itemSO.subdivision,
                itemSO.description,
                itemSO.website,
                itemSO.buildingItemId,
                itemSO.category,
                IconItemDBMapper().fromDBToDomain(itemI))
        }
    }

}