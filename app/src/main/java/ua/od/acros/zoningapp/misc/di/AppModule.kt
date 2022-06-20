package ua.od.acros.zoningapp.misc.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ua.od.acros.zoningapp.misc.data.JsonRepositoryImpl
import ua.od.acros.zoningapp.misc.data.LocationRepositoryImpl
import ua.od.acros.zoningapp.misc.data.ZonesRepositoryImpl
import ua.od.acros.zoningapp.misc.repository.JsonRepository
import ua.od.acros.zoningapp.misc.repository.LocationRepository
import ua.od.acros.zoningapp.misc.repository.ZonesRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindJsonRepository(citiesRepositoryImpl: JsonRepositoryImpl): JsonRepository

    @Binds
    abstract fun bindZonesRepository(zonesRepositoryImpl: ZonesRepositoryImpl): ZonesRepository

    @Binds
    abstract fun bindLocationRepository(locationRepositoryImpl: LocationRepositoryImpl): LocationRepository
}
