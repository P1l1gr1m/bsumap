package com.shist.data.roomDB.entities.buildingItem.structuralObjectItem.iconItem

import com.shist.data.model.StructuralObjectItemIconJson

// This mapper converts a JSON entity to a database entity
class IconItemJsonMapper {

    fun fromJsonToRoomDB(itemJson: StructuralObjectItemIconJson?, structuralObjectItemId: String?,
                         buildingItemId: String?) : IconItemEntityDB?
    {
        return if (itemJson == null) {
            null
        } else {
            IconItemEntityDB(itemJson.structuralObjectId!!,
                structuralObjectItemId,
                itemJson.subdivision,
                itemJson.logoPath,
                buildingItemId)
        }
    }

}