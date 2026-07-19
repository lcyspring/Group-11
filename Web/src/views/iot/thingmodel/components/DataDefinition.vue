<template>
  <!-- 属性 -->
  <template v-if="data.type === IoTThingModelTypeEnum.PROPERTY">
    <!-- 非列表型：数值 -->
    <div
      v-if="
        isNumericDataType(data.property.dataType)
      "
    >
      取值范围：{{ `${data.property.dataSpecs.min}~${data.property.dataSpecs.max}` }}
    </div>
    <!-- 非列表型：文本 -->
    <div v-if="IoTDataSpecsDataTypeEnum.TEXT === data.property.dataType">
      数据长度：{{ data.property.dataSpecs.length }}
    </div>
    <!-- 列表型: 数组、结构、时间（特殊） -->
    <div
      v-if="
        isSpecialDataType(data.property.dataType)
      "
    >
      -
    </div>
    <!-- 列表型: 布尔值、枚举 -->
    <div
      v-if="
        isBooleanOrEnumDataType(data.property.dataType)
      "
    >
      <div>
        {{ IoTDataSpecsDataTypeEnum.BOOL === data.property.dataType ? '布尔值' : '枚举值' }}：
      </div>
      <div v-for="item in data.property.dataSpecsList" :key="item.value">
        {{ `${item.name}-${item.value}` }}
      </div>
    </div>
  </template>
  <!-- 服务 -->
  <div v-if="data.type === IoTThingModelTypeEnum.SERVICE">
    调用方式：{{ getThingModelServiceCallTypeLabel(data.service!.callType) }}
  </div>
  <!-- 事件 -->
  <div v-if="data.type === IoTThingModelTypeEnum.EVENT">
    事件类型：{{ getEventTypeLabel(data.event!.type) }}
  </div>
</template>

<script lang="ts" setup>
import { ThingModelData } from '@/api/iot/thingmodel'
import {
  getEventTypeLabel,
  getThingModelServiceCallTypeLabel,
  IoTDataSpecsDataTypeEnum,
  IoTThingModelTypeEnum
} from '@/views/iot/utils/constants'

/** 数据定义展示组件 */
defineOptions({ name: 'DataDefinition' })

const isNumericDataType = (dataType?: string) =>
  ([IoTDataSpecsDataTypeEnum.INT, IoTDataSpecsDataTypeEnum.DOUBLE, IoTDataSpecsDataTypeEnum.FLOAT] as string[]).includes(dataType || '')
const isSpecialDataType = (dataType?: string) =>
  ([IoTDataSpecsDataTypeEnum.ARRAY, IoTDataSpecsDataTypeEnum.STRUCT, IoTDataSpecsDataTypeEnum.DATE] as string[]).includes(dataType || '')
const isBooleanOrEnumDataType = (dataType?: string) =>
  ([IoTDataSpecsDataTypeEnum.BOOL, IoTDataSpecsDataTypeEnum.ENUM] as string[]).includes(dataType || '')

defineProps<{ data: ThingModelData }>()
</script>

<style lang="scss" scoped></style>
