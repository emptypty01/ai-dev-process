<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { request } from '../api.js'
import { money } from '../money.js'

const props = defineProps({ initialExpandedId: { type: Number, default: null } })
const orders = ref([])
const loading = ref(true)
const cancelling = ref(null)
const error = ref('')
const notice = ref('')
const filter = ref('ALL')
const details = reactive({})
const visible = computed(() => orders.value.filter(order => filter.value === 'ALL' || order.status === filter.value))
const timestamp = value => new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value))

async function load() {
  loading.value = true
  error.value = ''
  try { orders.value = await request('/orders') }
  catch (failure) { error.value = failure.message }
  finally { loading.value = false }
}

async function fetchDetail(order) {
  const state = details[order.id]
  if (state.loading) return
  state.loading = true
  state.error = ''
  try { state.value = await request(`/orders/${order.id}`) }
  catch (failure) { state.error = failure.message }
  finally { state.loading = false }
}

async function toggle(order) {
  const state = details[order.id] ||= { expanded: false, loading: false, error: '', value: null }
  state.expanded = !state.expanded
  if (state.expanded && !state.value) await fetchDetail(order)
}

async function cancel(order) {
  if (cancelling.value !== null || !window.confirm(`确定取消订单“${order.orderNo}”？`)) return
  cancelling.value = order.id
  error.value = ''
  notice.value = ''
  try {
    const updated = await request(`/orders/${order.id}/cancel`, { method: 'POST' })
    orders.value = orders.value.map(item => item.id === updated.id ? updated : item)
    notice.value = '订单已取消，商品明细仍可查看。'
  } catch (failure) { error.value = failure.message }
  finally { cancelling.value = null }
}

onMounted(async () => {
  await load()
  const order = orders.value.find(item => item.id === props.initialExpandedId)
  if (order) await toggle(order)
})
</script>

<template>
  <div class="page-intro"><div><h1>订单</h1><p>查看订单，展开商品明细并核对下单时的价格。</p></div><a class="primary compact nav-button" href="#create">创建订单</a></div>
  <div v-if="error" role="alert" class="message error">{{ error }}</div>
  <div v-if="notice" role="status" class="message success">{{ notice }}</div>
  <section class="orders-panel" aria-labelledby="orders-title">
    <div class="panel-heading"><h2 id="orders-title">最近订单</h2><button class="quiet" :disabled="loading" @click="load">{{ loading ? '读取中…' : '刷新' }}</button></div>
    <div class="filters" aria-label="筛选订单状态"><button v-for="option in [{id:'ALL',label:'全部'},{id:'NEW',label:'待处理'},{id:'CANCELLED',label:'已取消'}]" :key="option.id" :class="{selected:filter === option.id}" :aria-pressed="filter === option.id" @click="filter = option.id">{{ option.label }}</button></div>
    <p v-if="loading" class="empty" role="status">正在读取订单…</p>
    <div v-else-if="!visible.length" class="empty"><strong>暂无{{ filter === 'NEW' ? '待处理' : filter === 'CANCELLED' ? '已取消' : '' }}订单</strong><p><a href="#create">选择商品，创建订单。</a></p></div>
    <div v-else class="table-scroll"><table class="order-table"><caption class="sr-only">最近100笔订单，使用展开按钮查看商品详情</caption>
      <thead><tr><th>订单号 / 时间</th><th>商品</th><th>合计</th><th>状态</th><th>操作</th></tr></thead>
      <tbody><template v-for="order in visible" :key="order.id">
        <tr class="expandable-order" @click="toggle(order)">
          <td><div class="order-heading"><button class="expand-button" :aria-label="`${details[order.id]?.expanded ? '收起' : '展开'}订单${order.orderNo}的商品详情`" :aria-expanded="Boolean(details[order.id]?.expanded)" :aria-controls="`detail-${order.id}`" @click.stop="toggle(order)"><span aria-hidden="true">{{ details[order.id]?.expanded ? '▾' : '▸' }}</span></button>
            <div><strong class="item-name">{{ order.orderNo }}</strong><time :datetime="order.createdAt">{{ timestamp(order.createdAt) }}</time></div></div></td>
          <td>{{ order.productCount }} 种 / {{ order.totalQuantity }} 件</td><td class="amount">{{ money(order.totalAmount) }}</td>
          <td><span class="status" :class="order.status === 'NEW' ? 'pending' : 'cancelled'">{{ order.status === 'NEW' ? '待处理' : '已取消' }}</span></td>
          <td @click.stop><button v-if="order.status === 'NEW'" class="text-button danger" :disabled="cancelling !== null" @click="cancel(order)">{{ cancelling === order.id ? '取消中…' : '取消订单' }}</button><span v-else class="muted">已结束</span></td>
        </tr>
        <tr v-if="details[order.id]?.expanded" :id="`detail-${order.id}`" class="detail-row"><td colspan="5">
          <div class="detail-content"><div class="detail-heading"><strong>下单商品明细</strong><span>商品后续修改不会影响此订单</span></div>
            <p v-if="details[order.id].loading" role="status">正在读取商品明细…</p>
            <div v-else-if="details[order.id].error" role="alert" class="message error">商品明细读取失败：{{ details[order.id].error }} <button class="text-button" @click="fetchDetail(order)">重新加载</button></div>
            <table v-else-if="details[order.id].value"><caption class="sr-only">订单{{ order.orderNo }}的下单商品快照</caption>
              <thead><tr><th>商品名称</th><th>下单类型</th><th>单价</th><th>数量</th><th>小计</th></tr></thead>
              <tbody><tr v-for="(line, index) in details[order.id].value.items" :key="index"><td>{{ line.productName }}</td><td>{{ line.productType || '历史商品（未记录类型）' }}</td><td>{{ money(line.unitPrice) }}</td><td>{{ line.quantity }}</td><td class="amount">{{ money(line.lineTotal) }}</td></tr></tbody>
            </table>
          </div>
        </td></tr>
      </template></tbody></table></div>
    <p class="table-note">显示最近100笔订单，金额以人民币计。点击订单主体或展开按钮查看商品详情。</p>
  </section>
</template>
