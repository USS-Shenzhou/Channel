#include "webrtc_ffm.h"

#include "api/audio/audio_processing.h"
#include "api/audio/audio_view.h"
#include "api/audio/builtin_audio_processing_builder.h"
#include "api/environment/environment_factory.h"
#include "api/scoped_refptr.h"
#include "common_audio/resampler/include/push_resampler.h"
#include "common_audio/vad/include/webrtc_vad.h"

struct ResamplerWrapper {
  webrtc::PushResampler<int16_t>* resampler;
  size_t num_channels;
};

extern "C" {

AudioProcessingHandle CreateAudioProcessing(void) {
  webrtc::scoped_refptr<webrtc::AudioProcessing> apm =
      webrtc::BuiltinAudioProcessingBuilder().Build(
          webrtc::CreateEnvironment());
  if (apm) {
    auto config = apm->GetConfig();
    config.pipeline.maximum_internal_processing_rate = 48000;
    apm->ApplyConfig(config);
    apm->AddRef();
    return apm.get();
  }
  return nullptr;
}

void FreeAudioProcessing(AudioProcessingHandle apm) {
  if (apm) {
    static_cast<webrtc::AudioProcessing*>(apm)->Release();
  }
}

void SetNoiseSuppression(AudioProcessingHandle apm, bool enabled, int level) {
  auto* ap = static_cast<webrtc::AudioProcessing*>(apm);
  auto config = ap->GetConfig();
  config.noise_suppression.enabled = enabled;
  config.noise_suppression.level =
      static_cast<webrtc::AudioProcessing::Config::NoiseSuppression::Level>(
          level);
  ap->ApplyConfig(config);
}

void SetEchoCanceller(AudioProcessingHandle apm,
                      bool enabled,
                      bool enforce_high_pass_filtering) {
  auto* ap = static_cast<webrtc::AudioProcessing*>(apm);
  auto config = ap->GetConfig();
  config.echo_canceller.enabled = enabled;
  config.echo_canceller.enforce_high_pass_filtering =
      enforce_high_pass_filtering;
  ap->ApplyConfig(config);
}

void SetHighPassFilter(AudioProcessingHandle apm, bool enabled) {
  auto* ap = static_cast<webrtc::AudioProcessing*>(apm);
  auto config = ap->GetConfig();
  config.high_pass_filter.enabled = enabled;
  ap->ApplyConfig(config);
}

void SetGainController(AudioProcessingHandle apm,
                       bool enabled,
                       float fixed_gain_db,
                       bool adaptive_enabled,
                       float headroom_db,
                       float max_gain_db,
                       float initial_gain_db,
                       float max_output_noise_level_dbfs,
                       float max_gain_change_db_per_second) {
  auto* ap = static_cast<webrtc::AudioProcessing*>(apm);
  auto config = ap->GetConfig();

  // 关闭旧版 GainController1 以避免冲突
  config.gain_controller1.enabled = false;

  // 映射到 GainController2
  config.gain_controller2.enabled = enabled;
  config.gain_controller2.fixed_digital.gain_db = fixed_gain_db;
  config.gain_controller2.adaptive_digital.enabled = adaptive_enabled;
  config.gain_controller2.adaptive_digital.headroom_db = headroom_db;
  config.gain_controller2.adaptive_digital.max_gain_db = max_gain_db;
  config.gain_controller2.adaptive_digital.initial_gain_db = initial_gain_db;
  config.gain_controller2.adaptive_digital.max_output_noise_level_dbfs =
      max_output_noise_level_dbfs;
  config.gain_controller2.adaptive_digital.max_gain_change_db_per_second =
      max_gain_change_db_per_second;

  ap->ApplyConfig(config);
}

void SetStreamDelayMs(AudioProcessingHandle apm, int delay_ms) {
  static_cast<webrtc::AudioProcessing*>(apm)->set_stream_delay_ms(delay_ms);
}

int ProcessStream(AudioProcessingHandle apm,
                  const int16_t* src,
                  int sample_rate,
                  int num_channels,
                  int16_t* dest) {
  auto* ap = static_cast<webrtc::AudioProcessing*>(apm);
  webrtc::StreamConfig config(sample_rate, num_channels);
  return ap->ProcessStream(src, config, config, dest);
}

int ProcessReverseStream(AudioProcessingHandle apm,
                         const int16_t* src,
                         int sample_rate,
                         int num_channels,
                         int16_t* dest) {
  auto* ap = static_cast<webrtc::AudioProcessing*>(apm);
  webrtc::StreamConfig config(sample_rate, num_channels);
  return ap->ProcessReverseStream(src, config, config, dest);
}

VadHandle CreateVad(void) {
  return WebRtcVad_Create();
}

void FreeVad(VadHandle vad) {
  if (vad) {
    WebRtcVad_Free(static_cast<VadInst*>(vad));
  }
}

int InitVad(VadHandle vad) {
  return WebRtcVad_Init(static_cast<VadInst*>(vad));
}

int SetVadMode(VadHandle vad, int mode) {
  return WebRtcVad_set_mode(static_cast<VadInst*>(vad), mode);
}

int ProcessVad(VadHandle vad,
               int sample_rate,
               const int16_t* audio_frame,
               size_t frame_length) {
  return WebRtcVad_Process(static_cast<VadInst*>(vad), sample_rate, audio_frame,
                           frame_length);
}

ResamplerHandle CreateResampler(int src_sample_rate,
                                int dst_sample_rate,
                                int num_channels) {
  auto* w = new ResamplerWrapper();
  w->num_channels = static_cast<size_t>(num_channels);
  w->resampler = new webrtc::PushResampler<int16_t>(
      static_cast<size_t>(src_sample_rate / 100),
      static_cast<size_t>(dst_sample_rate / 100), w->num_channels);
  return w;
}

void FreeResampler(ResamplerHandle resampler) {
  auto* w = static_cast<ResamplerWrapper*>(resampler);
  if (w) {
    delete w->resampler;
    delete w;
  }
}

void Resample(ResamplerHandle resampler,
              const int16_t* src,
              size_t src_frames,
              int16_t* dest,
              size_t dest_frames) {
  auto* w = static_cast<ResamplerWrapper*>(resampler);
  webrtc::InterleavedView<const int16_t> src_view(src, src_frames,
                                                  w->num_channels);
  webrtc::InterleavedView<int16_t> dest_view(dest, dest_frames,
                                             w->num_channels);
  w->resampler->Resample(src_view, dest_view);
}

}  // extern "C"