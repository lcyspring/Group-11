import assert from 'node:assert/strict'
import fs from 'node:fs'
import test from 'node:test'

const form = fs.readFileSync(new URL('./index.vue', import.meta.url), 'utf8')
const api = fs.readFileSync(
  new URL('../../../../api/crm/marketing/index.ts', import.meta.url),
  'utf8'
)
const controller = fs.readFileSync(
  new URL(
    '../../../../../../Server/mitedtsm-module-crm/src/main/java/com/meession/etm/module/crm/controller/admin/marketing/CrmMarketingController.java',
    import.meta.url
  ),
  'utf8'
)

test('campaign form uses real users and the current login user instead of a fixed id', () => {
  assert.match(form, /UserApi\.getSimpleUserList\(\)/)
  assert.match(form, /useUserStore\(\)\.getUser\.id/)
  assert.match(form, /v-for="user in userOptions"/)
  assert.doesNotMatch(form, /ownerUserId:\s*1\b/)
})

test('campaign edit preserves the server representation and draft delete is explicit', () => {
  assert.match(form, /formData\.value = await MarketingApi\.getCampaign\(id\)/)
  assert.match(form, /MarketingApi\.deleteCampaign\(id\)/)
  assert.match(api, /url: '\/crm\/marketing\/campaign\/delete'/)
  assert.match(controller, /@DeleteMapping\("\/campaign\/delete"\)/)
  assert.match(controller, /crm:marketing-campaign:delete/)
})
