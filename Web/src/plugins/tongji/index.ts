import router from '@/router'

window._hmt = window._hmt || []
const HM_ID = import.meta.env.VITE_APP_BAIDU_CODE
const isLocalhost = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1'

;(function () {
  if (!HM_ID || isLocalhost) {
    return
  }
  const hm = document.createElement('script')
  hm.src = 'https://hm.baidu.com/hm.js?' + HM_ID
  const s = document.getElementsByTagName('script')[0]
  s.parentNode.insertBefore(hm, s)
})()

router.afterEach(function (to) {
  if (!HM_ID || isLocalhost) {
    return
  }
  _hmt.push(['_trackPageview', to.fullPath])
})
