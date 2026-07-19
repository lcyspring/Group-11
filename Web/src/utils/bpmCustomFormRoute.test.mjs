import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'

import { isResolvedBusinessRoute, resolveCustomFormRoutePath } from './bpmCustomFormRoute.mjs'
import { filterProcessDefinitions } from './processDefinitionFilter.mjs'
import { resolveBusinessFormComponentPath } from './bpmBusinessFormComponent.mjs'

test('preserves explicit browser routes without legacy rewriting', () => {
  assert.equal(resolveCustomFormRoutePath('/crm/contract'), '/crm/contract')
  assert.equal(
    resolveCustomFormRoutePath('/bpm/collaboration/work-request?id=8#approval'),
    '/bpm/collaboration/work-request?id=8#approval'
  )
  assert.equal(resolveCustomFormRoutePath('/bpm/oa/leave/create'), '/bpm/oa/leave/create')
  assert.equal(resolveCustomFormRoutePath('/crm/contract/index'), '/crm/contract/index')
})

test('rejects empty, relative and protocol-relative custom form paths', () => {
  assert.equal(resolveCustomFormRoutePath(), undefined)
  assert.equal(resolveCustomFormRoutePath('   '), undefined)
  assert.equal(resolveCustomFormRoutePath('crm/contract'), undefined)
  assert.equal(resolveCustomFormRoutePath('//outside.example/form'), undefined)
})

test('distinguishes business routes from the catch-all 404 route', () => {
  assert.equal(isResolvedBusinessRoute(), false)
  assert.equal(isResolvedBusinessRoute({ matched: [] }), false)
  assert.equal(
    isResolvedBusinessRoute({ matched: [{ path: '/:pathMatch(.*)*' }] }),
    false
  )
  assert.equal(
    isResolvedBusinessRoute({ matched: [{ path: '/crm' }, { path: 'contract' }] }),
    true
  )
  assert.equal(isResolvedBusinessRoute({ matched: [{}] }), true)
})

test('filters process cards by category and localized name', () => {
  const definitions = [
    { category: 'CRM_SALES', name: '合同审批' },
    { category: 'CRM_FINANCE', name: '回款审批' },
    { category: 'CRM_FINANCE' }
  ]
  assert.deepEqual(filterProcessDefinitions(definitions, '', undefined), definitions)
  assert.deepEqual(filterProcessDefinitions(definitions, '审批', 'CRM_FINANCE'), [definitions[1]])
  assert.deepEqual(filterProcessDefinitions(definitions, '不存在', 'CRM_FINANCE'), [])
})

test('accepts only explicit source components and rejects browser routes', () => {
  assert.equal(resolveBusinessFormComponentPath(), undefined)
  assert.equal(resolveBusinessFormComponentPath('relative/view'), undefined)
  assert.equal(resolveBusinessFormComponentPath('/crm/receivable'), undefined)
  assert.equal(resolveBusinessFormComponentPath('/crm/receivable/index'), undefined)
  assert.equal(
    resolveBusinessFormComponentPath(' /crm/contract/detail/index.vue?tab=approval#history '),
    '/crm/contract/detail/index.vue'
  )
  assert.equal(resolveBusinessFormComponentPath('/custom/detail.tsx'), '/custom/detail.tsx')
  assert.equal(resolveBusinessFormComponentPath('/bpm/oa/leave/detail'), undefined)
  assert.equal(resolveBusinessFormComponentPath('//invalid/component'), undefined)
})

test('business form registration resolves one exact module instead of substring matching', async () => {
  const routerHelper = await readFile(new URL('./routerHelper.ts', import.meta.url), 'utf8')
  assert.match(routerHelper, /modules\[`\.\.\/views\$\{resolvedPath\}`\]/)
  assert.doesNotMatch(routerHelper, /item\.includes\(componentPath\)/)
})

test('tracked BPM examples distinguish browser create routes from view components', async () => {
  const expectedRoutes = new Map([
    ['bpm-provision-contract.example.yaml', ['/crm/contract', '/crm/contract/detail/index.vue']],
    ['bpm-provision-receivable.example.yaml', ['/crm/receivable', '/crm/receivable/detail/index.vue']],
    [
      'bpm-provision.example.yaml',
      ['/crm/reimbursement', '/crm/reimbursement/ReimbursementApprovalDetail.vue']
    ],
    [
      'bpm-provision-refund.example.yaml',
      ['/crm/receivable-refund', '/crm/refund/RefundApprovalDetail.vue']
    ],
    ['bpm-provision-leave.example.yaml', ['/bpm/oa/leave/create', '/bpm/oa/leave/detail.vue']],
    ['bpm-provision-trip.example.yaml', ['/bpm/oa/trip/create', '/bpm/oa/trip/detail.vue']],
    ['bpm-provision-loan.example.yaml', ['/bpm/oa/loan/create', '/bpm/oa/loan/detail.vue']],
    [
      'bpm-provision-customer-visit.example.yaml',
      ['/crm/customer-visit/create', '/crm/customerVisit/detail.vue']
    ],
    [
      'bpm-provision-work-request.example.yaml',
      ['/bpm/collaboration/work-request', '/bpm/oa/work-request/detail.vue']
    ]
  ])

  for (const [fileName, [createRoute, viewComponent]] of expectedRoutes) {
    const yaml = await readFile(new URL(`../../../podman/config/${fileName}`, import.meta.url), 'utf8')
    assert.match(yaml, new RegExp(`form_create_path: ${createRoute.replaceAll('/', '\\/')}(?:\\n|\\r)`))
    assert.match(yaml, new RegExp(`form_view_path: ${viewComponent.replaceAll('/', '\\/')}(?:\\n|\\r)`))
  }
})

test('stored list routes are repaired in data with an audit trail, not in the frontend', async () => {
  const repair = await readFile(
    new URL('../../../database/maintenance/repair/repair-bpm-custom-view-component-paths.sql', import.meta.url),
    'utf8'
  )
  assert.match(repair, /CREATE TABLE IF NOT EXISTS `bpm_custom_view_path_repair_record`/)
  assert.match(repair, /INSERT IGNORE INTO `bpm_custom_view_path_repair_record`/)
  assert.match(repair, /UPDATE `bpm_process_definition_info`/)
  assert.match(repair, /'\/crm\/contract\/index'/)
  assert.match(repair, /'\/crm\/contract\/detail2'/)
  assert.match(repair, /'\/crm\/receivable\/detail\/index'/)
  assert.match(repair, /'\/crm\/contract\/detail\/index\.vue'/)
})
