cmake_minimum_required(VERSION 3.4.1)
project( arp-spoof )

#add_library(arp-spoof SHARED src/main/cpp/arp-spoof.cpp)
add_executable(arp-spoof src/main/cpp/arp-spoof.cpp )

# Not a shared object but android studio only packages native components
# into apk if their name is lib.*so
set_target_properties( arp-spoof PROPERTIES OUTPUT_NAME "libarpspoof.so" )

###################### LIBTINS ######################
add_library(tins SHARED IMPORTED)

set_target_properties( tins PROPERTIES IMPORTED_LOCATION
                       # Provides the path to the library you want to import.
                       /media/nibiru/Storage/Projects-AS/ARPSpoof/app/libs/armeabi-v7a/libtins.so )

#include_directories( src/main/cpp/include/ )

###################### NATIVE-LIB ######################
# Include libraries needed for native component
target_link_libraries(arp-spoof android log tins )

