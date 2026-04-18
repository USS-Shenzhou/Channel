#pragma once

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

#ifdef _WIN32
#define FFM_EXPORT __declspec(dllexport)
#else
#define FFM_EXPORT __attribute__((visibility("default")))
#endif

#ifdef __cplusplus
extern "C" {
#endif

/* ── AudioProcessing ─────────────────────────────────────── */

typedef void* AudioProcessingHandle;

FFM_EXPORT AudioProcessingHandle CreateAudioProcessing(void);
FFM_EXPORT void FreeAudioProcessing(AudioProcessingHandle apm);

/* Noise Suppression.
   level: 0=Low, 1=Moderate, 2=High, 3=VeryHigh. */
FFM_EXPORT void SetNoiseSuppression(AudioProcessingHandle apm,
                                    bool enabled,
                                    int level);

/* Echo Canceller. */
FFM_EXPORT void SetEchoCanceller(AudioProcessingHandle apm,
                                 bool enabled,
                                 bool enforce_high_pass_filtering);

/* High-Pass Filter on capture audio. */
FFM_EXPORT void SetHighPassFilter(AudioProcessingHandle apm, bool enabled);

/* Gain Controller (maps to WebRTC GainController2).
   enabled:           master switch.
   fixed_gain_db:     fixed digital gain applied after adaptive stage (dB).
   adaptive_enabled:  enable adaptive digital AGC.
   Remaining params only take effect when adaptive_enabled=true. */
FFM_EXPORT void SetGainController(AudioProcessingHandle apm,
                                  bool enabled,
                                  float fixed_gain_db,
                                  bool adaptive_enabled,
                                  float headroom_db,
                                  float max_gain_db,
                                  float initial_gain_db,
                                  float max_output_noise_level_dbfs,
                                  float max_gain_change_db_per_second);

/* Stream delay for echo cancellation (milliseconds). */
FFM_EXPORT void SetStreamDelayMs(AudioProcessingHandle apm, int delay_ms);

/* Process one 10ms frame of capture (microphone) audio.
   src and dest may alias. Returns 0 on success. */
FFM_EXPORT int ProcessStream(AudioProcessingHandle apm,
                             const int16_t* src,
                             int sample_rate,
                             int num_channels,
                             int16_t* dest);

/* Process one 10ms frame of render (speaker) audio for AEC reference.
   Call BEFORE the corresponding ProcessStream. Returns 0 on success. */
FFM_EXPORT int ProcessReverseStream(AudioProcessingHandle apm,
                                    const int16_t* src,
                                    int sample_rate,
                                    int num_channels,
                                    int16_t* dest);

/* ── Voice Activity Detection ────────────────────────────── */

typedef void* VadHandle;

FFM_EXPORT VadHandle CreateVad(void);
FFM_EXPORT void FreeVad(VadHandle vad);
FFM_EXPORT int InitVad(VadHandle vad);

/* mode: 0=Quality, 1=LowBitrate, 2=Aggressive, 3=VeryAggressive. */
FFM_EXPORT int SetVadMode(VadHandle vad, int mode);

/* Returns: 1=voice, 0=silence, -1=error.
   sample_rate: 8000, 16000, or 32000 Hz.
   frame_length: samples per channel for 10/20/30 ms. */
FFM_EXPORT int ProcessVad(VadHandle vad,
                          int sample_rate,
                          const int16_t* audio_frame,
                          size_t frame_length);

/* ── Resampler ───────────────────────────────────────────── */

typedef void* ResamplerHandle;

/* Create for 10ms frames at the given rates and channel count. */
FFM_EXPORT ResamplerHandle CreateResampler(int src_sample_rate,
                                           int dst_sample_rate,
                                           int num_channels);
FFM_EXPORT void FreeResampler(ResamplerHandle resampler);

/* Resample one 10ms frame.
   src_frames / dest_frames = samples per channel = sample_rate / 100. */
FFM_EXPORT void Resample(ResamplerHandle resampler,
                         const int16_t* src,
                         size_t src_frames,
                         int16_t* dest,
                         size_t dest_frames);

#ifdef __cplusplus
}
#endif