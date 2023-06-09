package com.shist.data.retrofit

import com.shist.data.model.BuildingItemImageJson
import com.shist.data.model.BuildingItemJson
import retrofit2.http.GET
import retrofit2.http.Query

// This interface is needed for getting data from server with needed link (list of links is below)
interface MapDataApi{

    @GET("api/buildings/all")
    suspend fun getData() : List<BuildingItemJson>?

    @GET("api/building-photos")
    suspend fun getImagesWithBuildingId(@Query("buildingId") buildingId: String) : List<BuildingItemImageJson>

}

//base path:
//map.bsu.by:51107
//
// ----------------------
//
///api/buildings/all
//
//return all buildings info:
//- address with coordinates
//- type of buildings
//- all related structural objects with icons and categories
//
// ----------------------
//
///api/buildings/modern
//
//return only modern buildings (with the same info as above) which are listed here:
//https://docs.google.com/spreadsheets/d/1eORmT9kGK5tZ6WwoS9SYc5YPshkdYLIe/edit#gid=1698269244
//
// ----------------------
//
///api/buildings/historical
//
//
//return only historical buildings info:
//
// ----------------------
//
///api/building-photos?buildingId={id}
//
//return all building photos (work for both modern and historical buildings) for specified building id
//
// ----------------------
//
///api/structural-objects/all
//
//return all structural objects, which are listed here:
//https://docs.google.com/spreadsheets/d/1eORmT9kGK5tZ6WwoS9SYc5YPshkdYLIe/edit#gid=2004203366