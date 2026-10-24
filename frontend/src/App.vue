<script setup>
import { computed, onMounted, ref } from 'vue'

const orders = ref([])
const loading = ref(true)
const saving = ref(false)
const cancelling = ref(null)
const error = ref('')
const notice = ref('')
const filter = ref('ALL')
const form = ref({ itemName: '', quantity: 1, unitPrice: '' })
const visibleOrders = computed(() => orders.value.filter(order => filter.value === 'ALL' || order.status === filter.value))
const activeCount = computed(() => orders.value.filter(order => order.status === 'NEW').length)
const money = value => new Intl.NumberFormat('zh-CN', { style: 'currency', currency: 'CNY' }).format(value)
const timestamp = value => new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))

async function request(path, options) {
  const response = await fetch(`/api${path}`, options)
  const body = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(body.message || `请求失败（${response.status}），请稍后重试。`)
  return body
}

async function load() {
  loading.value = true
  error.value = ''
  try { orders.value = await request('/orders') }
  catch { error.value = '暂时无法读取订单，请确认服务已启动后点击“刷新”。' }
  finally { loading.value = false }
}

async function create() {
  if (saving.value) return
  saving.value = true
  error.value = ''
  notice.value = ''
  try {
    const order = await request('/orders', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ ...form.value, quantity: Number(form.value.quantity), unitPrice: Number(form.value.unitPrice) }),
    })
    orders.value = [order, ...orders.value].slice(0, 100)
    form.value = { itemName: '', quantity: 1, unitPrice: '' }
    notice.value = '订单已创建。'
  } catch (failure) { error.value = failure.message }
  finally { saving.value = false }
}

async function cancel(order) {
  if (!window.confirm(`确定取消“${order.itemName}”的订单？`)) return
  cancelling.value = order.id
  error.value = ''
  notice.value = ''
  try {
    const updated = await request(`/orders/${order.id}/cancel`, { method: 'POST' })
    orders.value = orders.value.map(item => item.id === updated.id ? updated : item)
    notice.value = '订单已取消。'
  } catch (failure) { error.value = failure.message }
  finally { cancelling.value = null }
}

onMounted(load)
</script>

<template>
  <header class="topbar">
    <a class="brand" href="/" aria-label="订单管理首页"><span class="brand-mark" aria-hidden="true">订</span>订单管理</a>
    <span class="workspace">本地演示工作区</span>
  </header>
  <main>
    <div class="page-intro">
      <div><h1>订单</h1><p>登记商品订单，核对金额并跟进处理状态。</p></div>
      <div class="count"><strong>{{ activeCount }}</strong><span>笔待处理</span></div>
    </div>
    <div v-if="error" role="alert" class="message error">{{ error }}</div>
    <div v-if="notice" role="status" class="message success">{{ notice }}</div>
    <div class="workspace-grid">
      <section class="orders-panel" aria-labelledby="orders-title">
        <div class="panel-heading"><h2 id="orders-title">最近订单</h2><button class="quiet" :disabled="loading" @click="load">{{ loading ? '读取中…' : '刷新' }}</button></div>
        <div class="filters" aria-label="筛选订单状态">
          <button v-for="option in [{ id: 'ALL', label: '全部' }, { id: 'NEW', label: '待处理' }, { id: 'CANCELLED', label: '已取消' }]"
            :key="option.id" :class="{ selected: filter === option.id }" :aria-pressed="filter === option.id" @click="filter = option.id">{{ option.label }}</button>
        </div>
        <p v-if="loading" class="empty" role="status">正在读取订单…</p>
        <div v-else-if="!visibleOrders.length" class="empty"><strong>暂无{{ filter === 'CANCELLED' ? '已取消' : filter === 'NEW' ? '待处理' : '' }}订单</strong><p>在右侧填写商品信息，创建第一笔订单。</p></div>
        <div v-else class="table-scroll">
          <table>
            <caption class="sr-only">最近 100 笔订单</caption>
            <thead><tr><th scope="col">商品 / 订单号</th><th scope="col">数量</th><th scope="col">总金额</th><th scope="col">状态</th><th scope="col">操作</th></tr></thead>
            <tbody><tr v-for="order in visibleOrders" :key="order.id">
              <td><strong class="item-name">{{ order.itemName }}</strong><span class="order-no" :title="order.orderNo">{{ order.orderNo }}</span><time :datetime="order.createdAt">{{ timestamp(order.createdAt) }}</time></td>
              <td>{{ order.quantity }}</td><td class="amount">{{ money(order.totalAmount) }}</td>
              <td><span class="status" :class="order.status === 'NEW' ? 'pending' : 'cancelled'">{{ order.status === 'NEW' ? '待处理' : '已取消' }}</span></td>
              <td><button v-if="order.status === 'NEW'" class="cancel" :disabled="cancelling !== null" @click="cancel(order)">{{ cancelling === order.id ? '取消中…' : '取消订单' }}</button><span v-else class="muted">已结束</span></td>
            </tr></tbody>
          </table>
        </div>
        <p class="table-note">显示最近 100 笔订单 · 金额以人民币计</p>
      </section>
      <aside class="create-panel" aria-labelledby="create-title">
        <h2 id="create-title">创建订单</h2><p class="form-intro">总金额将按数量与单价自动计算。</p>
        <form @submit.prevent="create">
          <label for="item-name">商品名称</label><input id="item-name" v-model="form.itemName" name="itemName" required maxlength="80" placeholder="例如：机械键盘" autocomplete="off" />
          <div class="form-row"><div><label for="quantity">数量</label><input id="quantity" v-model="form.quantity" name="quantity" type="number" min="1" max="1000" step="1" required /></div>
          <div><label for="unit-price">单价（元）</label><input id="unit-price" v-model="form.unitPrice" name="unitPrice" type="number" min="0.01" max="99999999.99" step="0.01" required placeholder="0.00" /></div></div>
          <button class="primary" type="submit" :disabled="saving">{{ saving ? '创建中…' : '创建订单' }}</button>
        </form>
        <p class="form-hint">取消订单后会保留记录，方便后续核对。</p>
      </aside>
    </div>
  </main>
  <footer>订单管理示例 · 供本地流程演练使用</footer>
</template>
