import { readonly, ref } from 'vue'

const refreshRevision = ref(0)

export const customerGarbageRefreshRevision = readonly(refreshRevision)

export const invalidateCustomerGarbageList = () => {
  refreshRevision.value += 1
}
