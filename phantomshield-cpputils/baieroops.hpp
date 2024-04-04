//
// Created by Baier on 2024/4/4.
//

#ifndef JVMACQUIRER_BAIEROOPS_H
#define JVMACQUIRER_BAIEROOPS_H


#include <cstdint>
#include <string>
#include "jvm_internal.hpp"

#define FIELDINFO_TAG_SIZE             2
#define FIELDINFO_TAG_BLANK            0
#define FIELDINFO_TAG_OFFSET           1
#define FIELDINFO_TAG_TYPE_PLAIN       2
#define FIELDINFO_TAG_TYPE_CONTENDED   3
#define FIELDINFO_TAG_MASK             3

namespace baieroops {
    static inline void init();
};

namespace offset{
    inline uintptr_t class_offset;
}



namespace java_hotspot {
    template<typename T>
    class array {
    public:
        int64_t get_length() {
            if (sizeof(T) != 0x08) {
                return *reinterpret_cast<int *>(reinterpret_cast<uintptr_t>(this));
            }
            static auto typeArray = JVMWrappers::find_type_fields("Array<Klass*>");
            if (!typeArray.has_value()) return 0;
            static auto lengthEntry = typeArray.value().get()["_length"];
            return *reinterpret_cast<int64_t *>(reinterpret_cast<uintptr_t>(this) + lengthEntry->offset);
        }

        T *get_data() {
            if (sizeof(T) != 0x08) {
                static auto typeArray = JVMWrappers::find_type_fields("Array<u2>");
                if (!typeArray.has_value()) return nullptr;
                static auto dataEntry = typeArray.value().get()["_data"];
                return reinterpret_cast<T *>(reinterpret_cast<uintptr_t>(this) + dataEntry->offset);
            }
            static auto typeArray = JVMWrappers::find_type_fields("Array<Klass*>");
            if (!typeArray.has_value()) return nullptr;
            static auto dataEntry = typeArray.value().get()["_data"];
            return reinterpret_cast<T *>(reinterpret_cast<uintptr_t>(this) + dataEntry->offset);
        }

        auto at(int i) -> T {
            if (i >= 0 && i < this->get_length())
                return static_cast<T>(this->get_data()[i]);
            return static_cast<T>(NULL);
        }

        [[nodiscard]] auto is_empty() const -> bool {
            return get_length() == 0;
        }

        T *adr_at(const int i) {
            if (i >= 0 && i < this->get_length())
                return &this->get_data()[i];
            return nullptr;
        }
    };
}



namespace java_hotspot {
    class symbol {
    public:
        auto base() -> void *;

        auto to_string() -> std::string;
    };
}



namespace java_hotspot {
    class const_pool {
    public:
        auto get_base() -> void **;

        auto get_tags() -> array<unsigned char> *;

        auto get_length() -> int;

        auto get_symbol_at(int index) -> symbol *;

        auto get_symbol_at_address(int index) -> symbol **;

        auto get_pool_holder() -> void *;
    };
}



namespace java_hotspot {
    enum FieldOffset {
        access_flags_offset = 0,
        name_index_offset = 1,
        signature_index_offset = 2,
        initval_index_offset = 3,
        low_packed_offset = 4,
        high_packed_offset = 5,
        field_slots = 6
    };

    class field_info {
    public:
        static auto from_field_array(array<uint16_t>* fields, int index) -> field_info *;

        static auto from_field_array(uint16_t *fields, int index) -> field_info *;

        [[nodiscard]] auto get_shorts() -> uint16_t *;

        auto get_access_flags() -> uint16_t;

        auto get_name_index() -> uint16_t;

        auto get_signature_index() -> uint16_t;

        auto get_initval_index() -> uint16_t;

        auto get_offset() -> uint32_t;

        auto get_name(const_pool *const_pool) -> symbol *;

        auto get_signature(const_pool *const_pool) -> symbol *;

        auto is_internal() -> bool;

        static auto lookup_symbol(int index) -> symbol *;

    };
}


namespace java_hotspot {
    class instance_klass {
    public:
        auto get_name() -> symbol *;

        auto get_fields() -> array<uint16_t> *;

        auto get_constants() -> const_pool *;

        auto get_super_klass() -> instance_klass *;

        auto find_field_info(
                const std::string &field_name,
                const std::string &field_signature
        ) -> std::tuple<field_info *, instance_klass *>;
    };

    auto get_static_object_field(
            field_info *field_info,
            instance_klass *current_klass
    ) -> jobject;

    auto set_static_object_field(
            field_info *field_info,
            instance_klass *current_klass,
            jobject obj
    ) -> void;

    /* The pointers obtained by this function are not managed by the JVM, so it will cause a memory leak if not managed properly. */
    auto get_objet_field_detach(
            field_info *field_info,
            jobject obj
    ) -> jobject;

    auto get_objet_field(
            field_info *field_info,
            jobject obj
    ) -> jobject;

    auto set_objet_field(
            field_info *field_info,
            jobject obj,
            jobject value
    ) -> void;
}

namespace vm_symbols {
    auto get_symbol() -> java_hotspot::symbol **;

    auto symbol_at(int index) -> java_hotspot::symbol *;
}



#endif //JVMACQUIRER_BAIEROOPS_H
