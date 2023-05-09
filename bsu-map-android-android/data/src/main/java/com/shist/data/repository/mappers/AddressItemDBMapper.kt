package com.shist.data.repository.mappers

import com.shist.data.roomDB.entities.buildingItem.adressItem.AddressItemEntityDB
import com.shist.domain.AddressItem

// This mapper converts a database entity to a domain entity
class AddressItemDBMapper {

    fun fromDBToDomain(item: AddressItemEntityDB?) : AddressItem? {
        return if (item == null) {
            null
        } else {
            AddressItem(item.id,
                item.description,
                item.latitude,
                item.longitude)
        }
    }

}