<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import ProductPage from './components/ProductPage.vue'
import CreateOrderPage from './components/CreateOrderPage.vue'
import OrderPage from './components/OrderPage.vue'
import { money } from './money.js'

const currentView = () => ['products', 'create'].includes(location.hash.slice(1)) ? location.hash.slice(1) : 'orders'
const view = ref(currentView())
const notice = ref('')
const createdId = ref(null)
function route() { view.value = currentView() }
function created(order) {
  createdId.value = order.id
  notice.value = `订单 ${order.orderNo} 已创建，实际合计 ${money(order.totalAmount)}。`
  location.hash = 'orders'
}
onMounted(() => window.addEventListener('hashchange', route))
onUnmounted(() => window.removeEventListener('hashchange', route))
</script>

<template>
  <header class="topbar"><a class="brand" href="#orders" aria-label="订单管理首页"><span class="brand-mark" aria-hidden="true">订</span>订单管理</a>
    <nav class="main-nav" aria-label="主导航"><a href="#orders" :class="{active:view !== 'products'}" :aria-current="view !== 'products' ? 'page' : undefined">订单</a><a href="#products" :class="{active:view === 'products'}" :aria-current="view === 'products' ? 'page' : undefined">商品配置</a></nav>
    <span class="workspace">本地演示工作区</span></header>
  <main><div v-if="notice" role="status" class="message success">{{ notice }} <button class="text-button" aria-label="关闭创建成功提示" @click="notice = ''">关闭</button></div>
    <ProductPage v-if="view === 'products'" /><CreateOrderPage v-else-if="view === 'create'" @created="created" /><OrderPage v-else :initial-expanded-id="createdId" />
  </main>
  <footer>订单管理示例 · 供本地流程演练使用</footer>
</template>
