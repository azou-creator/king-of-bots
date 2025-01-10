
export default {
  state: {
    is_recording: false,
    a_steps: "",
    b_steps: "",
    record_loser: "none",
  },
  getters: {
    
  },
  mutations: {
    updateIsRecording(state, is_recording) {
      state.is_recording = is_recording;
    },
    updateSteps(state, { a_steps, b_steps }) {
      state.a_steps = a_steps;
      state.b_steps = b_steps;
    },
    updateRecordLoser(state, record_loser) {
      state.record_loser = record_loser;
    },
  },
  actions: {},
};
