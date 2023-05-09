package com.shist.data.roomDB.entities.buildingItem.buildingItemImage

import com.shist.data.model.BuildingItemImageJson

// This mapper converts a JSON entity to a database entity
class BuildingItemImageJsonMapper {

    fun fromJsonToRoomDB(itemJson: BuildingItemImageJson?) : BuildingItemImageEntityDB?
    {
        return if (itemJson == null) {
            null
        } else {
            BuildingItemImageEntityDB((itemJson.buildingId!! + "/" + itemJson.imagePath),
                itemJson.description,
                itemJson.imagePath,
                itemJson.buildingId)
        }
    }

}