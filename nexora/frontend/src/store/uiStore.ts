import { create } from 'zustand';

interface BottomSheetState {
  isOpen: boolean;
  type: 'QUICK_ACTIONS' | 'FILTER' | 'REPLY' | null;
  data?: any;
}

interface ReportModalState {
  isOpen: boolean;
  email?: any;
}

interface UIStore {
  bottomSheet: BottomSheetState;
  reportModal: ReportModalState;
  sidebarCollapsed: boolean;
  pageTitle: string | null;
  pageSubtitle: string | null;
  installPrompt: {
    isAvailable: boolean;
    deferredPrompt: any;
  };
  openBottomSheet: (type: BottomSheetState['type'], data?: any) => void;
  closeBottomSheet: () => void;
  openReportModal: (email: any) => void;
  closeReportModal: () => void;
  toggleSidebar: () => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  setPageChrome: (title: string | null, subtitle?: string | null) => void;
  clearPageChrome: () => void;
  setInstallPrompt: (prompt: any) => void;
}

export const useUIStore = create<UIStore>((set) => ({
  bottomSheet: { isOpen: false, type: null },
  reportModal: { isOpen: false },
  sidebarCollapsed: false,
  pageTitle: null,
  pageSubtitle: null,
  installPrompt: { isAvailable: false, deferredPrompt: null },

  openBottomSheet: (type, data) => set({ bottomSheet: { isOpen: true, type, data } }),
  closeBottomSheet: () => set({ bottomSheet: { isOpen: false, type: null, data: undefined } }),

  openReportModal: (email) => set({ reportModal: { isOpen: true, email } }),
  closeReportModal: () => set({ reportModal: { isOpen: false, email: undefined } }),

  toggleSidebar: () => set((state) => ({ sidebarCollapsed: !state.sidebarCollapsed })),
  setSidebarCollapsed: (collapsed) => set({ sidebarCollapsed: collapsed }),
  setPageChrome: (title, subtitle = null) => set({ pageTitle: title, pageSubtitle: subtitle }),
  clearPageChrome: () => set({ pageTitle: null, pageSubtitle: null }),
  setInstallPrompt: (prompt) => set({ installPrompt: { isAvailable: !!prompt, deferredPrompt: prompt } }),
}));
