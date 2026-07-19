<script lang="ts" setup>
import { propTypes } from '@/utils/propTypes'
import { getIconData, iconToSVG, replaceIDs } from '@iconify/utils'
import { useDesign } from '@/hooks/web/useDesign'
import offlineCollections from './offline-icon-collections.generated.json'

const offlineCollectionsByPrefix = new Map(
  offlineCollections.map((collection) => [collection.prefix, collection as any])
)

defineOptions({ name: 'Icon' })

const { getPrefixCls } = useDesign()

const prefixCls = getPrefixCls('icon')

const props = defineProps({
  // icon name
  icon: propTypes.string,
  // icon color
  color: propTypes.string,
  // icon size
  size: propTypes.number.def(16),
  // icon svg class
  svgClass: propTypes.string.def('')
})

const elRef = ref<ElRef>(null)

const isLocal = computed(() => props.icon?.startsWith('svg-icon:'))

const symbolId = computed(() => {
  return unref(isLocal) ? `#icon-${props.icon.split('svg-icon:')[1]}` : props.icon
})

const getIconifyStyle = computed(() => {
  const { color, size } = props
  return {
    fontSize: `${size}px`,
    height: '1em',
    color
  }
})

const getSvgClass = computed(() => {
  const { svgClass } = props
  return `iconify ${svgClass}`
})

const updateIcon = async (icon: string) => {
  if (unref(isLocal)) return

  await nextTick()

  const el = unref(elRef)
  if (!el) return
  if (!icon) return

  const [prefix, name] = icon.split(':', 2)
  const collection = offlineCollectionsByPrefix.get(prefix)
  const iconData = collection ? getIconData(collection, name) : null
  if (iconData) {
    const rendered = iconToSVG(iconData, {})
    const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    for (const [attribute, value] of Object.entries(rendered.attributes)) {
      svg.setAttribute(attribute, String(value))
    }
    svg.setAttribute('aria-hidden', 'true')
    svg.setAttribute('class', getSvgClass.value)
    svg.innerHTML = replaceIDs(rendered.body)
    el.textContent = ''
    el.appendChild(svg)
  } else {
    el.textContent = ''
    el.dataset.missingIcon = icon
  }
}

watch(
  () => props.icon,
  (icon: string) => {
    updateIcon(icon)
  },
  { immediate: true, flush: 'post' }
)
</script>

<template>
  <ElIcon :class="prefixCls" :color="color" :size="size">
    <svg v-if="isLocal" :class="getSvgClass">
      <use :xlink:href="symbolId" />
    </svg>

    <span v-else ref="elRef" :class="$attrs.class" :style="getIconifyStyle"></span>
  </ElIcon>
</template>
