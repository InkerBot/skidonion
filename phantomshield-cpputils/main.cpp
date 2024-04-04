//
// Created by Baier on 2024/4/4.
//

#include <iostream>
#include "baieroops.hpp"
#include "jni_md.h"


extern "C" JNIIMPORT VMStructEntry *gHotSpotVMStructs;
extern "C" JNIIMPORT VMTypeEntry *gHotSpotVMTypes;
extern "C" JNIIMPORT VMIntConstantEntry *gHotSpotVMIntConstants;
extern "C" JNIIMPORT VMLongConstantEntry *gHotSpotVMLongConstants;

void baieroops::init() {

    JVMWrappers::init(gHotSpotVMStructs, gHotSpotVMTypes, gHotSpotVMIntConstants, gHotSpotVMLongConstants);
    const auto java_lang_Class = JVMWrappers::find_type_fields("java_lang_Class");
    if (!java_lang_Class.has_value()) {
        //Handle unexpected error
    }

    offset::class_offset = *reinterpret_cast<jint*>(java_lang_Class.value().get()["_klass_offset"]->address);



}

java_hotspot::instance_klass *baieroops::get_instance_klass_from_jclass(jclass clz) {
    if (clz == nullptr)
        return nullptr;

    /* Dereference class */
    void *klass_ptr = *reinterpret_cast<void **>(clz);
    if (klass_ptr == nullptr)
        return nullptr;

    // Get the instance klass
    klass_ptr = *reinterpret_cast<void **>(reinterpret_cast<uintptr_t>(klass_ptr) + global_offsets::klass_offset);
    return static_cast<java_hotspot::instance_klass *>(klass_ptr);
}

jfieldID baieroops::get_field_id(jclass clz, const char *name, const char *sign, bool isStatic) {
    auto klass = baieroops::get_instance_klass_from_jclass(clz);
    auto info = klass->find_field_info(name,sign);
    auto field = std::get<0>(info);
    auto holder = std::get<1>(info);
    if (!isStatic){
        return java_hotspot::instance_klass::to_instance_jfieldID(field->get_offset());
    }
    return holder->to_static_jfieldID(field->get_offset());
}


auto java_hotspot::symbol::to_string() -> std::string {
    static auto type_symbol = JVMWrapper::from_instance("Symbol", this).value();
    const uint16_t length = *type_symbol.get_field<uint16_t>("_length").value();
    const char *body = type_symbol.get_field<char>("_body").value();
    return std::string{body, static_cast<std::string::size_type>(length)};
}

auto java_hotspot::instance_klass::get_constants() -> const_pool * {
    static VMStructEntry *_constants_entry = JVMWrappers::find_type_fields("InstanceKlass").value().get()["_constants"];
    if (!_constants_entry) return nullptr;
    return *reinterpret_cast<const_pool **>(reinterpret_cast<uint8_t *>(this) + _constants_entry->offset);
}


auto java_hotspot::instance_klass::get_name() -> symbol * {
    static VMStructEntry *_name_entry = JVMWrappers::find_type_fields("Klass").value().get()["_name"];
    if (!_name_entry)
        return nullptr;
    return *reinterpret_cast<symbol **>(reinterpret_cast<uint8_t *>(this) + _name_entry->offset);
}

auto java_hotspot::instance_klass::find_field_info(
        const std::string &field_name,
        const std::string &field_signature
) -> std::tuple<field_info *, instance_klass *> {
    auto current_klass = this;
    while (current_klass) {
        const auto fields = current_klass->get_fields();
        const auto fields_length = fields->get_length() / field_slots;
        const auto fields_data = fields->get_data();
        const auto name_and_signature_param = field_name + field_signature;
        const auto constants = current_klass->get_constants();
        for (auto i = 0; i < fields_length; i++) {
            const auto field = field_info::from_field_array(fields_data, i);
            if (!field) {
                continue;
            }
            if (
                const auto name_and_signature_field = field->get_name(constants)->to_string() +
                                                      field->get_signature(constants)->to_string();
                    !name_and_signature_param._Equal(name_and_signature_field)
                    ) {
                continue;
            }
            return {field, current_klass};
        }
        current_klass = current_klass->get_super_klass();
    }
    throw std::runtime_error("find_field_error");
}



auto java_hotspot::instance_klass::get_super_klass() -> instance_klass * {
    static VMStructEntry *_super_entry = JVMWrappers::find_type_fields("Klass").value().get()["_super"];
    if (!_super_entry) return nullptr;
    return *reinterpret_cast<instance_klass **>(reinterpret_cast<uint8_t *>(this) + _super_entry->offset);
}

auto java_hotspot::instance_klass::get_fields() -> array<uint16_t> * {
    static VMStructEntry *_fields_entry = JVMWrappers::find_type_fields("InstanceKlass").value().get()["_fields"];
    if (!_fields_entry) return nullptr;
    return *reinterpret_cast<array<uint16_t> **>(reinterpret_cast<uint8_t *>(this) + _fields_entry->offset);
}

void java_hotspot::instance_klass::set_jni_ids(java_hotspot::JNIid *ids) {
    static VMStructEntry *_jni_ids_entry = JVMWrappers::find_type_fields("InstanceKlass").value().get()["_jni_ids"];
    if (!_jni_ids_entry) return;
    *reinterpret_cast<JNIid **>(reinterpret_cast<uint8_t *>(this) + _jni_ids_entry->offset) = ids;
}

auto java_hotspot::instance_klass::jni_ids() -> java_hotspot::JNIid * {
    static VMStructEntry *_jni_ids_entry = JVMWrappers::find_type_fields("InstanceKlass").value().get()["_jni_ids"];
    if (!_jni_ids_entry) return nullptr;
    return *reinterpret_cast<JNIid **>(reinterpret_cast<uint8_t *>(this) + _jni_ids_entry->offset);
}


auto java_hotspot::instance_klass::encode_klass_hash(int offset) -> uintptr_t {
    uintptr_t klass_hash = 0;
    auto current_klass = this;
    while (!klass_hash) {
        const auto fields = current_klass->get_fields();
        const auto fields_length = fields->get_length() / field_slots;
        const auto fields_data = fields->get_data();

        for (auto i = 0; i < fields->get_length(); i++) {
            const auto field = field_info::from_field_array(fields_data, i);
            if (!field) {
                continue;
            }
            if (field->get_offset() == offset){
                klass_hash = current_klass->identity_hash();
            }
        }
        current_klass = current_klass->get_super_klass();
    }

    return ((klass_hash & klass_mask) << klass_shift) | checked_mask_in_place;
}

auto java_hotspot::instance_klass::to_instance_jfieldID(int offset) -> jfieldID {
    intptr_t as_uint = ((offset & large_offset_mask) << offset_shift) | instance_mask_in_place;
    auto result = (jfieldID) as_uint;
    return result;
}

auto java_hotspot::instance_klass::to_static_jfieldID(int offset) -> jfieldID {
    JNIid *id = this->jni_id_for_offset(offset);
    return(jfieldID) id;;
}

auto java_hotspot::instance_klass::jni_id_for_offset(int offset) -> java_hotspot::JNIid * {
    JNIid* probe = jni_ids() == nullptr ? nullptr : jni_ids()->find(offset);
    if (probe == nullptr) {
        probe = new JNIid(this, offset, this->jni_ids());
        this->set_jni_ids(probe);
    }
    return probe;
}




auto java_hotspot::const_pool::get_base() -> void ** {
    static VMTypeEntry *ConstantPool_entry = JVMWrappers::find_type("ConstantPool").value();
    if (!ConstantPool_entry) return nullptr;
    return reinterpret_cast<void **>(reinterpret_cast<uint8_t *>(this) + ConstantPool_entry->size);
}

auto java_hotspot::const_pool::get_tags() -> array<unsigned char> * {
    auto contatnPool = JVMWrapper::from_instance("ConstantPool", this).value();
    const auto tag = *contatnPool.get_field<void *>("_tags").value();
    return static_cast<array<unsigned char> *>(tag);
}

auto java_hotspot::const_pool::get_length() -> int {
    static VMStructEntry *length_entry = JVMWrappers::find_type_fields("ConstantPool").value().get()["_length"];
    if (!length_entry) return 0;
    return *reinterpret_cast<int *>(reinterpret_cast<uint8_t *>(this) + length_entry->offset);
}

auto java_hotspot::const_pool::get_symbol_at(const int index) -> symbol * {
    return *get_symbol_at_address(index);
}

auto java_hotspot::const_pool::get_symbol_at_address(const int index) -> symbol ** {
    return reinterpret_cast<symbol **>(&get_base()[index]);
}

auto java_hotspot::const_pool::get_pool_holder() -> void * {
    static VMStructEntry *pool_holder_entry = JVMWrappers::find_type_fields("ConstantPool").value().get()[
            "_pool_holder"];
    if (!pool_holder_entry) return nullptr;
    return *reinterpret_cast<void **>(reinterpret_cast<uint8_t *>(this) + pool_holder_entry->offset);
}

auto java_hotspot::field_info::from_field_array(array<uint16_t> *fields, const int index) -> field_info * {
    return reinterpret_cast<field_info *>(fields->adr_at(index * field_slots));
}

java_hotspot::field_info *java_hotspot::field_info::from_field_array(uint16_t *fields, const int index) {
    return reinterpret_cast<field_info *>(fields + index * field_slots);
}

auto java_hotspot::field_info::get_shorts() -> uint16_t * {
    return reinterpret_cast<uint16_t *>(reinterpret_cast<uint8_t *>(this));
}

auto java_hotspot::field_info::get_access_flags() -> uint16_t {
    return get_shorts()[access_flags_offset];
}

auto java_hotspot::field_info::get_name_index() -> uint16_t {
    return get_shorts()[name_index_offset];
}

auto java_hotspot::field_info::get_signature_index() -> uint16_t {
    return get_shorts()[signature_index_offset];
}

auto java_hotspot::field_info::get_initval_index() -> uint16_t {
    return get_shorts()[initval_index_offset];
}

auto java_hotspot::field_info::get_offset() -> uint32_t {
    const auto shorts = get_shorts();
    switch (const auto lower = shorts[low_packed_offset]; lower & FIELDINFO_TAG_MASK) {
        case FIELDINFO_TAG_OFFSET:
            return build_int_from_shorts(shorts[low_packed_offset], shorts[high_packed_offset]) >> FIELDINFO_TAG_SIZE;
        default:
            return 0;
    }
}

auto java_hotspot::field_info::get_name(const_pool *const_pool) -> symbol * {
    const int index = get_name_index();
    if (is_internal()) {
        return lookup_symbol(index);
    }
    return const_pool->get_symbol_at(index);
}

auto java_hotspot::field_info::get_signature(const_pool *const_pool) -> symbol * {
    const int index = get_signature_index();
    if (is_internal()) {
        return lookup_symbol(index);
    }
    return const_pool->get_symbol_at(index);
}

auto java_hotspot::field_info::is_internal() -> bool {
    return (get_access_flags() & jvm_internal::JVM_ACC_FIELD_INTERNAL) != 0;
}

auto java_hotspot::field_info::lookup_symbol(const int index) -> symbol * {
    return vm_symbols::symbol_at(index);
}

auto vm_symbols::get_symbol() -> java_hotspot::symbol ** {
    static auto reference_wrapper = JVMWrappers::find_type_fields("vmSymbols");
    if (!reference_wrapper.has_value()) {
        /*std::cout << "Failed to find vmSymbols" << std::endl;*/

    }
    return *static_cast<java_hotspot::symbol ***>(reference_wrapper.value().get()["_symbols"]->
            address);
}

auto vm_symbols::symbol_at(const int index) -> java_hotspot::symbol * {
    return get_symbol()[index];
}

java_hotspot::JNIid *java_hotspot::JNIid::get_next() {
    static VMStructEntry *_next_entry = JVMWrappers::find_type_fields("JNIid").value().get()[
            "_next"];
    if (!_next_entry) return nullptr;
    return *reinterpret_cast<JNIid **>(reinterpret_cast<uint8_t *>(this) + _next_entry->offset);
}

int java_hotspot::JNIid::get_offset() {
    static VMStructEntry *_offset_entry = JVMWrappers::find_type_fields("JNIid").value().get()[
            "_offset"];
    if (!_offset_entry) return 0;
    return *reinterpret_cast<int*>(reinterpret_cast<uint8_t *>(this) + _offset_entry->offset);
}

java_hotspot::JNIid::JNIid(java_hotspot::instance_klass *pKlass, int i, java_hotspot::JNIid *pIid) {
    set_holder(pKlass);
    set_offset(i);
    set_next(pIid);
}

void java_hotspot::JNIid::set_offset(int offset) {
    static VMStructEntry *_offset_entry = JVMWrappers::find_type_fields("JNIid").value().get()[
            "_offset"];
    if (!_offset_entry) return;
     *reinterpret_cast<int*>(reinterpret_cast<uint8_t *>(this) + _offset_entry->offset) = offset;
}

void java_hotspot::JNIid::set_holder(java_hotspot::instance_klass *klass) {
    static VMStructEntry *_offset_entry = JVMWrappers::find_type_fields("JNIid").value().get()[
            "_holder"];
    if (!_offset_entry) return;
    *reinterpret_cast<instance_klass**>(reinterpret_cast<uint8_t *>(this) + _offset_entry->offset) = klass;
}

void java_hotspot::JNIid::set_next(java_hotspot::JNIid *next) {
    static VMStructEntry *_next_entry = JVMWrappers::find_type_fields("JNIid").value().get()[
            "_next"];
    if (!_next_entry) return;
    *reinterpret_cast<JNIid **>(reinterpret_cast<uint8_t *>(this) + _next_entry->offset) = next;
}
