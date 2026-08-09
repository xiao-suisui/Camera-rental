const listeners = {}

export default {
  on(event, fn) {
    if (!listeners[event]) listeners[event] = []
    listeners[event].push(fn)
  },
  off(event, fn) {
    if (!listeners[event]) return
    listeners[event] = fn
      ? listeners[event].filter(f => f !== fn)
      : []
  },
  emit(event, data) {
    if (listeners[event]) {
      listeners[event].forEach(fn => fn(data))
    }
  }
}
