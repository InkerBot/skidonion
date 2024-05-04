#include <baieroops.h>
#include <iostream>
#include <format>
#include <jvm_internal.h>
#include "baieroops.h"
#include <klass.h>

#ifdef ENABLELOG

#define BEGIN_LOG(stuff) std::cout<< stuff

#define END_LOG << std::endl;

#define FLUSH std::cout.flush();

#else

#define BEGIN_LOG(stuff)

#define END_LOG

#define FLUSH
#endif


extern "C" JNIIMPORT VMStructEntry *gHotSpotVMStructs;
extern "C" JNIIMPORT VMTypeEntry *gHotSpotVMTypes;
extern "C" JNIIMPORT VMIntConstantEntry *gHotSpotVMIntConstants;
extern "C" JNIIMPORT VMLongConstantEntry *gHotSpotVMLongConstants;



auto InitGlobalOffsets() -> void {
    /* .\hotspot\src\share\vm\classfile\javaClasses.hpp -> class java_lang_Class : AllStatic */
    const auto java_lang_Class = JVMWrappers::find_type_fields("java_lang_Class");
    if (!java_lang_Class.has_value()) {             
        BEGIN_LOG("Failed to find java_lang_Class") END_LOG
    }

    /* java_lang_Class -> _klass_offset */
    //global_offsets::klass_offset = *static_cast<jint *>(java_lang_Class.value().get()["_klass_offset"]->address);
    //java_hotspot::bytecode_start_offset = java_hotspot::const_method::get_const_method_length();
}


auto InitJVMAcquirer() -> void {
    JVMWrappers::init(gHotSpotVMStructs, gHotSpotVMTypes, gHotSpotVMIntConstants, gHotSpotVMLongConstants);
    //InitJVMThread();
    InitGlobalOffsets();

    /*auto sub = debug_accessor->get_env()->FindClass("SubClass");
    auto subInstance = java_interop::get_instance_class(sub);*/


}

java_hotspot::instance_klass *get_instance_class(_jclass *const klasas) {
    /* Check if class is null */
    if (klasas == nullptr)
        return nullptr;

    /* Dereference class */
    void *klass_ptr = *reinterpret_cast<void **>(klasas);
    if (klass_ptr == nullptr)
        return nullptr;

    // Get the instance klass
    klass_ptr = *reinterpret_cast<void **>(reinterpret_cast<uintptr_t>(klass_ptr) + global_offsets::klass_offset);
    return static_cast<java_hotspot::instance_klass *>(klass_ptr);
}


auto get_jfield_id(jclass klass, const char * field_name, const char * field_sign, bool is_static) -> jfieldID
{
    auto const instance = get_instance_class(klass);

    return instance->get_field_id(field_name,field_sign,is_static);
}