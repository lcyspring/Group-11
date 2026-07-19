import request from '@/config/axios'

export interface ProductCategoryVO {
  id: number
  name: string
  parentId: number
}

export interface ProductCategoryListReqVO {
  name?: string | null
  parentId?: number
  createTime?: string
}

// 查询产品分类详情
export const getProductCategory = (id: number) =>
  request.get<ProductCategoryVO>({ url: '/crm/product-category/get', params: { id } })

// 新增产品分类
export const createProductCategory = (data: ProductCategoryVO) =>
  request.post<number>({ url: '/crm/product-category/create', data })

// 修改产品分类
export const updateProductCategory = (data: ProductCategoryVO) =>
  request.put<boolean>({ url: '/crm/product-category/update', data })

// 删除产品分类
export const deleteProductCategory = (id: number) =>
  request.delete<boolean>({ url: '/crm/product-category/delete', params: { id } })

// 产品分类列表
export const getProductCategoryList = (params: ProductCategoryListReqVO = {}) =>
  request.get<ProductCategoryVO[]>({ url: '/crm/product-category/list', params })
