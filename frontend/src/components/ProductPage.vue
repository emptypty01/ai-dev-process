<script setup>
import { computed, onMounted, ref } from 'vue'
import { jsonBody, request } from '../api.js'
import { money } from '../money.js'

const products = ref([])
const loading = ref(true)
const saving = ref(false)
const changing = ref(null)
const editing = ref(null)
const search = ref('')
const error = ref('')
const notice = ref('')
const form = ref({ type: '', name: '', price: '' })
const visible = computed(() => products.value.filter(p => `${p.name} ${p.type} ${p.id}`.includes(search.value.trim())))

async function load() {
  loading.value = true
  error.value = ''
  try { products.value = await request('/products') }
  catch (failure) { error.value = failure.message }
  finally { loading.value = false }
}

function edit(product) {
  editing.value = product?.id ?? null
  form.value = product ? { type: product.type, name: product.name, price: product.price } : { type: '', name: '', price: '' }
  error.value = ''
}

async function save() {
  if (saving.value || changing.value !== null) return
  saving.value = true
  error.value = ''
  notice.value = ''
  const id = editing.value
  try {
    const saved = await request(id ? `/products/${id}` : '/products', jsonBody(id ? 'PUT' : 'POST', form.value))
    products.value = [saved, ...products.value.filter(p => p.id !== saved.id)].sort((a, b) => b.id - a.id)
    notice.value = id ? '商品已更新，已有订单保持原资料和价格。' : '商品已新增并启用。'
    edit(null)
  } catch (failure) { error.value = failure.message }
  finally { saving.value = false }
}

async function toggle(product) {
  if (saving.value || changing.value !== null) return
  changing.value = product.id
  error.value = ''
  notice.value = ''
  try {
    const updated = await request(`/products/${product.id}/status`, jsonBody('PUT', { enabled: !product.enabled }))
    products.value = products.value.map(p => p.id === updated.id ? updated : p)
    notice.value = updated.enabled ? '商品已启用，可用于新订单。' : '商品已停用，已有订单仍可查看。'
  } catch (failure) { error.value = failure.message }
  finally { changing.value = null }
}

onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h1>商品配置</h1><p>维护商品资料与售价，启用的商品可以用于创建订单。</p></div>
    <button class="primary compact" :disabled="saving" @click="edit(null)">新增商品</button></div>
  <div v-if="error" role="alert" class="message error">{{ error }}</div>
  <div v-if="notice" role="status" class="message success">{{ notice }}</div>
  <div class="catalog-grid">
    <section class="orders-panel" aria-labelledby="catalog-title">
      <div class="panel-heading"><h2 id="catalog-title">商品目录</h2><button class="quiet" :disabled="loading" @click="load">刷新</button></div>
      <div class="search-row"><label for="product-search" class="sr-only">搜索商品名称、类型或编号</label>
        <input id="product-search" v-model="search" type="search" placeholder="搜索名称或类型" /><span>共 {{ products.length }} 个商品</span></div>
      <p v-if="loading" class="empty" role="status">正在读取商品…</p>
      <div v-else-if="!visible.length" class="empty"><strong>{{ search ? '没有匹配的商品' : '暂无商品' }}</strong>
        <p>{{ search ? '请调整搜索条件。' : '填写右侧资料，添加第一个商品。' }}</p></div>
      <div v-else class="table-scroll"><table><caption class="sr-only">商品配置列表</caption>
        <thead><tr><th>名称</th><th>类型</th><th>售价</th><th>状态</th><th>操作</th></tr></thead>
        <tbody><tr v-for="product in visible" :key="product.id">
          <td><strong class="item-name">{{ product.name }}</strong><span class="order-no">P-{{ product.id }}</span></td>
          <td>{{ product.type }}</td><td class="amount">{{ money(product.price) }}</td>
          <td><span class="status" :class="product.enabled ? 'enabled' : 'cancelled'">{{ product.enabled ? '已启用' : '已停用' }}</span></td>
          <td><div class="actions"><button class="text-button" :disabled="saving" @click="edit(product)">编辑</button>
            <button class="text-button" :class="{ danger: product.enabled }" :disabled="saving || changing !== null" @click="toggle(product)">
              {{ changing === product.id ? '处理中…' : product.enabled ? '停用' : '启用' }}</button></div></td>
        </tr></tbody></table></div>
      <p class="table-note">停用商品不会删除，也不会改变已有订单。</p>
    </section>
    <aside class="create-panel" aria-labelledby="product-form-title">
      <h2 id="product-form-title">{{ editing ? '编辑商品' : '新增商品' }}</h2>
      <p class="form-intro">{{ editing ? `P-${editing} · 修改仅影响新订单` : '新商品默认启用' }}</p>
      <form @submit.prevent="save"><fieldset :disabled="saving">
        <label for="product-type">商品类型</label><input id="product-type" v-model.trim="form.type" required maxlength="40" placeholder="例如：办公设备" />
        <label for="product-name" class="spaced-label">商品名称</label><input id="product-name" v-model.trim="form.name" required maxlength="80" placeholder="例如：机械键盘" />
        <label for="product-price" class="spaced-label">价格（元）</label>
        <input id="product-price" v-model="form.price" type="number" min="0.01" max="99999999.99" step="0.01" required placeholder="0.00" />
        <div class="form-actions"><button class="primary" type="submit" :disabled="changing !== null">{{ saving ? '保存中…' : editing ? '保存修改' : '保存商品' }}</button>
          <button v-if="editing" class="quiet" type="button" @click="edit(null)">取消</button></div>
      </fieldset></form>
      <p class="form-hint">已创建订单保留原名称、类型和价格。</p>
    </aside>
  </div>
</template>
