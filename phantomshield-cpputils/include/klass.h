//
// Created by Administrator on 2024/3/15.
//

#ifndef KLASS_H
#define KLASS_H

#include "const_pool.h"
#include "symbol.h"
#include "array.h"
#include "field_info.h"

namespace java_hotspot {
    class JNIid;
    class instance_klass {
    public:
        auto get_name() -> symbol *;

        auto get_constants() -> const_pool *;

        auto get_fields() -> array<uint16_t> *;

        auto jni_ids() -> JNIid*;

        auto jni_id_for_offset(int offset) -> JNIid*;

        void set_jni_ids(JNIid* ids);

        static auto to_instance_jfieldID(int offset) -> jfieldID ;

        auto to_static_jfieldID(int offset) -> jfieldID ;

        auto get_field_id(const char* name,const char* sign,bool isStatic) -> jfieldID;

        auto get_method_id(const char* name,const char* sign,bool isStatic) -> jmethodID;

        auto get_super_klass() -> instance_klass *;

        auto find_field_info(
            const std::string &field_name,
            const std::string &field_signature
        ) -> std::tuple<field_info *, instance_klass *>;
    };
}


#endif //KLASS_H
