package com.shist.data.roomDB

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.shist.data.roomDB.entities.Converters
import com.shist.data.roomDB.entities.buildingItem.BuildingItemDAO
import com.shist.data.roomDB.entities.buildingItem.BuildingItemEntityDB
import com.shist.data.roomDB.entities.buildingItem.adressItem.AddressItemEntityDB
import com.shist.data.roomDB.entities.buildingItem.buildingItemImage.BuildingItemImageEntityDB
import com.shist.data.roomDB.entities.buildingItem.structuralObjectItem.StructuralObjectItemEntityDB
import com.shist.data.roomDB.entities.buildingItem.structuralObjectItem.iconItem.IconItemEntityDB

@Database(entities = [BuildingItemEntityDB::class, StructuralObjectItemEntityDB::class, BuildingItemImageEntityDB::class,
    AddressItemEntityDB::class, IconItemEntityDB::class], version = 2)
@TypeConverters(Converters::class)
abstract class BuildingItemsDatabase : RoomDatabase() {
    abstract fun buildingItemsDao(): BuildingItemDAO
}