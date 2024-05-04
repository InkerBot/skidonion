#pragma once
#include <jni.h>

auto InitJVMAcquirer() -> void;

auto get_jfield_id(jclass klass,const char* field_name,const char* field_sign,bool is_static) -> jfieldID;