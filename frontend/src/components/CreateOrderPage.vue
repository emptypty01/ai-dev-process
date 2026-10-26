<script setup>
import { computed, onMounted, ref } from 'vue'
import { jsonBody, request } from '../api.js'
import { formatCents, lineCents, money } from '../money.js'

const emit = defineEmits(['created'])
const products = ref([])
const selected = ref([])
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const search = ref('')
const visible = computed(() => products.value.filter(p => `${p.name} ${p.type} ${p.id}`.includes(search.value.trim())))
const total = computed(() => selected.value.reduce((sum, line) => sum + lineCents(line.product.price, line.quantity), 0n))
const totalQuantity = computed(() => selected.value.reduce((sum, line) => sum + (Number.isInteger(line.quantity) ? line.quantity : 0), 0))
const added = id => selected.value.some(line => line.product.id === id)

async function load() {
  loading.value = true
  error.value = ''
  try { products.value = await request('/products?enabled=true') }
  catch (failure) { error.value = failure.message }
  finally { loading.value = false }
}

function add(product) {
  if (!added(product.id)) selected.value.push({ product, quantity: 1 })
}

async function create() {
  if (saving.value || !selected.value.length) return
  if (selected.value.some(line => !Number.isInteger(line.quantity) || line.quantity < 1 || line.quantity > 1000)) {
    error.value = '每种商品数量须为1–1000的整数。'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const order = await request('/orders', jsonBody('POST', { items: selected.value.map(line => ({ productId: line.product.id, quantity: line.quantity })) }))
    emit('created', order)
  } catch (failure) { error.value = failure.message }
  finally { saving.value = false }
}

onMounted(load)
</script>

<template>
  <div class="page-intro"><div><h1>创建订单</h1><p>选择已启用商品，并分别设置数量。金额按下单时的商品价格计算。</p></div><a class="quiet nav-button" href="#orders">返回订单</a></div>
  <div v-if="error" role="alert" class="message error">{{ error }} <button class="text-button" @click="load">刷新商品</button></div>
  <div class="checkout-grid">
    <section class="orders-panel" aria-labelledby="choose-title">
      <div class="panel-heading"><h2 id="choose-title">选择商品</h2><button class="quiet" :disabled="loading" @click="load">刷新</button></div>
      <div class="search-row"><label class="sr-only" for="choose-search">搜索商品名称、类型或编号</label><input id="choose-search" v-model="search" type="search" placeholder="搜索商品名称或类型" /></div>
      <p v-if="loading" class="empty" role="status">正在读取商品…</p>
      <div v-else-if="!visible.length" class="empty"><strong>{{ products.length ? '没有匹配商品' : '还没有可用商品' }}</strong>
        <p>先添加并启用商品，再创建订单。<a href="#products">前往商品配置</a></p></div>
      <div v-else class="table-scroll"><table><caption class="sr-only">已启用的可选商品</caption>
        <thead><tr><th>商品</th><th>类型</th><th>单价</th><th>操作</th></tr></thead>
        <tbody><tr v-for="product in visible" :key="product.id"><td><strong class="item-name">{{ product.name }}</strong><span class="order-no">P-{{ product.id }}</span></td>
          <td>{{ product.type }}</td><td class="amount">{{ money(product.price) }}</td><td><button class="text-button" :disabled="saving || added(product.id)" :aria-label="`${added(product.id) ? '已添加' : '添加'}${product.name}`" @click="add(product)">{{ added(product.id) ? '已添加' : '添加' }}</button></td>
        </tr></tbody></table></div>
      <p class="table-note">仅显示已启用商品。<a href="#products">前往商品配置</a></p>
    </section>
    <section class="orders-panel" aria-labelledby="selected-title">
      <div class="panel-heading"><h2 id="selected-title">已选商品</h2><span class="muted">{{ selected.length }} 种商品</span></div>
      <p v-if="!selected.length" class="empty">请在左侧添加至少一个商品。</p>
      <form @submit.prevent="create"><fieldset :disabled="saving">
        <div v-for="line in selected" :key="line.product.id" class="cart-line">
          <div><strong>{{ line.product.name }}</strong><p class="muted">{{ line.product.type }} / {{ money(line.product.price) }}</p></div>
          <div class="quantity-control"><label :for="`quantity-${line.product.id}`">数量</label><input :id="`quantity-${line.product.id}`" v-model.number="line.quantity" type="number" min="1" max="1000" step="1" required /></div>
          <div class="line-amount"><strong>{{ formatCents(lineCents(line.product.price, line.quantity)) }}</strong>
            <button class="text-button" type="button" :aria-label="`移除${line.product.name}`" @click="selected = selected.filter(item => item.product.id !== line.product.id)">移除</button></div>
        </div>
        <div class="checkout-total"><span>{{ selected.length }} 种商品，共 {{ totalQuantity }} 件</span><strong>{{ formatCents(total) }}</strong></div>
        <div class="checkout-submit"><button class="primary" type="submit" :disabled="!selected.length">{{ saving ? '创建中…' : '创建订单' }}</button>
          <p class="form-hint">下单后保存商品信息与价格，后续修改不会影响本订单。</p></div>
      </fieldset></form>
    </section>
  </div>
</template>
