use std::cell::RefCell;

thread_local! {
    static ORG_ID: RefCell<Option<String>> = RefCell::new(None);
}

pub fn set_org(org: String) {
    ORG_ID.with(|o| *o.borrow_mut() = Some(org));
}

pub fn get_org() -> Option<String> {
    ORG_ID.with(|o| o.borrow().clone())
}
